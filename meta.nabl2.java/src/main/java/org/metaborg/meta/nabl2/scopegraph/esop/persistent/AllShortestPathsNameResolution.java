package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasOccurrence.occurrenceEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.OpenCounter;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.tuples.HasLabel;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.OccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, java.io.Serializable {

    private static final long serialVersionUID = 42L;

    private static final boolean DEBUG = false;

    private final PersistentScopeGraph<S, L, O> scopeGraph;

    private final Set.Immutable<L> labels;
    private final L labelD;
    private final IRegExpMatcher<L> wf;

    private final IRelation<L> ordered;
    private final IRelation<L> unordered;

    private final OpenCounter<S, L> scopeCounter;

    transient private java.util.Map<IRelation<L>, EnvironmentBuilder<S, L, O>> environmentBuilderCache;

    public AllShortestPathsNameResolution(PersistentScopeGraph<S, L, O> scopeGraph, IResolutionParameters<L> params,
            OpenCounter<S, L> scopeCounter) {
        this.scopeGraph = scopeGraph;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(params.getLabels()));
        this.labelD = params.getLabelD();
        this.wf = RegExpMatcher.create(params.getPathWf());
        this.ordered = params.getSpecificityOrder();
        assert ordered.getDescription().equals(
                RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";
        this.unordered = new Relation<>(RelationDescription.STRICT_PARTIAL_ORDER);
        this.scopeCounter = scopeCounter;

        initTransients();

        // stage and cache environment builders
        getEnvironmentBuilder(ordered);
        getEnvironmentBuilder(unordered);
        
        unresolvedImports = scopeGraph.requireImportEdgeStream().collect(CapsuleCollectors.toSet());
        resolvedImports = Map.Immutable.of();
        
        initAllShortestPaths();
    }

    private void initTransients() {
        this.environmentBuilderCache = Maps.newHashMap();
    }
    
    private Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImports = Set.Immutable.of();

    private Map.Immutable<ScopeLabelOccurrence<S, L, O>, OccurrenceLabelScope<O, L, S>> resolvedImports = Map.Immutable.of();
    
    private Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> substitutionEvidence = Map.Immutable.of();    

    private ScopeLabelScope<S, L, O> toDirectEdge(ScopeLabelOccurrence<S, L, O> requireImportEdge,
            OccurrenceLabelScope<O, L, S> associatedScopeEdge) {
        assert Objects.equals(requireImportEdge.label(), associatedScopeEdge.label());

        return ImmutableScopeLabelScope.of(requireImportEdge.scope(), requireImportEdge.label(),
                associatedScopeEdge.scope());
    }
    
    private void initAllShortestPaths() {
        final List<O> rs = scopeGraph.sourceEdgeStream().map(tuple -> tuple.occurrence()).sorted()
                .collect(Collectors.toList());

        final List<O> ds = scopeGraph.targetEdgeStream().map(tuple -> tuple.occurrence()).sorted()
                .collect(Collectors.toList());

        final List<S> scopes = scopeGraph.getAllScopes().stream().sorted().collect(Collectors.toList());

        final List<Object> nodes = new ArrayList<>();
        nodes.addAll(rs);
        nodes.addAll(ds);
        nodes.addAll(scopes);

        final java.util.Map<Integer, Object> forwardIndex = new HashMap<>();
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            forwardIndex.put(nodeIndex, nodes.get(nodeIndex));
        }

        final java.util.Map<Object, Integer> reverseIndex = new HashMap<>();
        for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
            reverseIndex.put(nodes.get(nodeIndex), nodeIndex);
        }

        @SuppressWarnings("unchecked")
        final Distance<L>[][] dist = new Distance[nodes.size()][nodes.size()];
        final int[][] next = new int[nodes.size()][nodes.size()];

        /*
         * 1 let dist be a |V| × |V| array of minimum distances initialized to ∞
         * (infinity) 2 for each vertex v 3 dist[v][v] ← 0 4 for each edge (u,v)
         * 5 dist[u][v] ← w(u,v) // the weight of the edge (u,v) 6 for k from 1
         * to |V| 7 for i from 1 to |V| 8 for j from 1 to |V| 9 if dist[i][j] >
         * dist[i][k] + dist[k][j] 10 dist[i][j] ← dist[i][k] + dist[k][j] 11
         * end if
         */

        /*
         * let dist be a |V| × |V| array of minimum distances initialized to ∞
         */
        for (int n = 0; n < nodes.size(); n++) {
            for (int m = 0; m < nodes.size(); m++) {
                dist[n][m] = Distance.INFINITE;
                next[n][m] = -1;
            }
        }

        /*
         * for each vertex v { dist[v][v] <- 0 }
         */
        for (int n = 0; n < nodes.size(); n++) {
            dist[n][n] = Distance.ZERO;
        }

        /*
         * Injecting all direct edges that resulted from resolving import
         * clauses.
         */
        substitutionEvidence.keySet().forEach(sls -> {
            final int sIndex = reverseIndex.get(sls.sourceScope());
            final int tIndex = reverseIndex.get(sls.targetScope());
            
            dist[sIndex][tIndex] = Distance.of(sls.label());
            next[sIndex][tIndex] = tIndex;        
        });
        
        /*
         * Assigning the weight of the edge (u,v).
         * 
         * for each edge (u,v) { dist[u][v] <- w(u,v) }
         */
        scopeGraph.sourceEdgeStream().forEach(slo -> {
            final int oIndex = reverseIndex.get(slo.occurrence());
            final int sIndex = reverseIndex.get(slo.scope());

            dist[oIndex][sIndex] = Distance.of(slo.label());
            next[oIndex][sIndex] = sIndex;
        });
        scopeGraph.middleEdgeStream().forEach(sls -> {
            final int sIndex = reverseIndex.get(sls.sourceScope());
            final int tIndex = reverseIndex.get(sls.targetScope());
            
            dist[sIndex][tIndex] = Distance.of(sls.label());
            next[sIndex][tIndex] = tIndex;
        });
        scopeGraph.targetEdgeStream().forEach(slo -> {
            final int sIndex = reverseIndex.get(slo.scope());
            final int oIndex = reverseIndex.get(slo.occurrence());

            dist[sIndex][oIndex] = Distance.of(slo.label());
            next[sIndex][oIndex] = oIndex;
        });
        
        // TODO: use configurable comparators based resolution, reachability and ..
        final Comparator<Distance<L>> comparator = new PathComparator<>(ordered);
        
        for (int k = 0; k < nodes.size(); k++) {
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = 0; j < nodes.size(); j++) {
                    Distance<L> ij = dist[i][j];
                    Distance<L> ik = dist[i][k];
                    Distance<L> kj = dist[k][j];

                    // TODO: use configurable BiFunction, curried by 'wf'
                    Distance<L> ikj = Distance.concat(wf, ik, kj);

                    if (comparator.compare(ij, ikj) > 0) {
                        dist[i][j] = ikj;
                        next[i][j] = next[i][k];
                        
                        if (DEBUG) {
                            if (forwardIndex.get(i) instanceof IOccurrence
                                    && forwardIndex.get(j) instanceof IOccurrence) {
                                
                                final String distString = String.format("dist: %s -> %s = %s", nodes.get(i),
                                        nodes.get(j), ikj);
                                final String nextString = String.format("next: %s -> %s = %s", nodes.get(i),
                                        nodes.get(j), nodes.get(next[i][k]));

                                System.out.println(distString);
                                System.out.println(nextString);
                                System.out.println();
                            }
                        }
                    }
                }
            }
        }

        this.dist = dist;
        this.next = next;
        
        this.forwardIndex = forwardIndex;
        this.reverseIndex = reverseIndex;       
        
        final Set.Transient<ScopeLabelOccurrence<S, L, O>> __unresolvedImports = Set.Transient.of();
        final Map.Transient<ScopeLabelOccurrence<S, L, O>, OccurrenceLabelScope<O, L, S>> __resolvedImports = resolvedImports
                .asTransient();
        final Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> __substitutionEvidence = substitutionEvidence
                .asTransient();        
        
        for (ScopeLabelOccurrence<S, L, O> unresolvedImport : unresolvedImports) {
            if (tryResolve(unresolvedImport.occurrence()).isPresent()) {
                // System.out.println("Import resolved.");
                
                // TODO support multiple paths
                final Set.Immutable<IResolutionPath<S, L, O>> declarationPaths = resolve(unresolvedImport.occurrence());
                
                final IResolutionPath<S, L, O> path = declarationPaths.findFirst().get();                               

                // @formatter:off
                OccurrenceLabelScope<O, L, S> associatedScopeEdge = scopeGraph.associatedScopeEdgeStream()
                        .filter(occurrenceEquals(path.getDeclaration()))
                        .findAny().get();
                // @formatter:on
                
                __resolvedImports.__put(unresolvedImport, associatedScopeEdge);
                
                //// ************ /////
                
                final ScopeLabelScope<S, L, O> directEdge = toDirectEdge(unresolvedImport, associatedScopeEdge);
                __substitutionEvidence.put(directEdge, path);                
            } else {
                // System.out.println("Import not resolvable.");
                
                __unresolvedImports.__insert(unresolvedImport);
            }               
        }
        
        if (resolvedImports.size() == __resolvedImports.size()) {
            this.resolvedImports = __resolvedImports.freeze();
            this.unresolvedImports =  __unresolvedImports.freeze();
            this.substitutionEvidence = __substitutionEvidence.freeze();
            return;
        } else {
            this.resolvedImports = __resolvedImports.freeze();
            this.unresolvedImports =  __unresolvedImports.freeze();
            this.substitutionEvidence = __substitutionEvidence.freeze();
            
            initAllShortestPaths();
        }
    }

    /*
     * Reconstruct path from all-shortest-paths matrix.
     */
    private Optional<IResolutionPath<S, L, O>> path(final O reference, final O declaration) {

        final int u = reverseIndex.get(reference);

        int j = u;
        final int k = reverseIndex.get(declaration);

        /*
         * if next[u][v] = null then return [] path = [u] while u ≠ v u ←
         * next[u][v] path.append(u) return path
         */

        final List<Object> trace = new ArrayList<>(0);

        if (next[j][k] == -1) {
            return Optional.empty();
        } else {
            trace.add(forwardIndex.get(j));

            while (j != k) {
                j = next[j][k];
                trace.add(forwardIndex.get(j));
            }
        }
        
        if (DEBUG) {
            final List<?> __states = trace.stream().collect(Collectors.toList());
            final List<L> __labels = dist[u][k].labels.stream().collect(Collectors.toList());
        }
        
        @SuppressWarnings("unchecked")
        final List<S> states = trace.stream().skip(1).limit(trace.size() - 2).map(scope -> (S) scope)
                .collect(Collectors.toList());
        final List<L> labels = dist[u][k].labels.stream().limit(states.size()).collect(Collectors.toList());

        final IScopePath<S, L, O> pathStart = Paths.empty(states.get(0));
        final IScopePath<S, L, O> pathMiddle = IntStream.range(1, states.size())
                .mapToObj(i -> ImmutableTuple2.of(labels.get(i), states.get(i)))
                .reduce(pathStart, (pathIntermediate, tuple) -> {                
                    final ScopeLabelScope<S, L, O> query = ImmutableScopeLabelScope.of(pathIntermediate.getTarget(),
                            tuple._1(), tuple._2());
                    
                    if (substitutionEvidence.containsKey(query)) {
                        final IResolutionPath<S, L, O> importPath = substitutionEvidence.get(query);
                        return Paths.append(pathIntermediate,
                                Paths.named(pathIntermediate.getTarget(), tuple._1(), importPath, tuple._2())).get();
                    } else {
                        return Paths.append(pathIntermediate,
                                Paths.direct(pathIntermediate.getTarget(), tuple._1(), tuple._2())).get();                        
                    }                    
                }, (p1, p2) -> Paths.append(p1, p2).get());
            
        final Optional<IResolutionPath<S, L, O>> resolutionPath = Paths.resolve(reference, pathMiddle, declaration);
        return resolutionPath;
    }
    
    public static class PathComparator<L extends ILabel> implements Comparator<Distance<L>> {
        
        private final IRelation<L> labelOrder;

        public PathComparator(IRelation<L> labelOrder) {
            this.labelOrder = labelOrder;
        }

        @Override
        public int compare(Distance<L> o1, Distance<L> o2) {
            if (o1 == o2) {
                 return 0;
            }
            if (o1 == Distance.ZERO) {
                return -1;
            }
            if (o1 == Distance.INFINITE) {
                return +1;
            }            
            if (o2 == Distance.ZERO) {
                return +1;
            }
            if (o2 == Distance.INFINITE) {
                return -1;
            }
            
            int commonLength = Math.min(o1.labels.size(), o2.labels.size());
            
            final OptionalInt commonComparisonResult = IntStream.range(0, commonLength).map(index -> {
                L l1 = o1.labels.get(index);
                L l2 = o2.labels.get(index);

                if (!labelOrder.contains(l1, l2)) {
                    return 0;
                } else {
                    if (labelOrder.smaller(l2).contains(l1)) {
                        return -1;
                    } else {
                        return +1;
                    }
                }
            }).filter(comparisionResult -> comparisionResult != 0).findFirst();

            if (commonComparisonResult.isPresent()) {
                return commonComparisonResult.getAsInt();
            }
            
            if (o1.labels.size() == o2.labels.size()) {
                return 0;
            } else {
                if (o1.labels.size() < o2.labels.size()) {
                    return -1;
                } else {
                    return +1;
                }
            }
        }
    }
    
    public static class Distance<L extends ILabel> implements java.io.Serializable { // Comparable<Distance<L>>

        private static final long serialVersionUID = 42L;
        
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance ZERO = new Distance(Collections.EMPTY_LIST);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance INFINITE = new Distance(Collections.EMPTY_LIST);

        private final List<L> labels;

        public static final <L extends ILabel> Distance<L> of(L label) {
            return new Distance<L>(label);
        }
        
        public static final <L extends ILabel> Distance<L> concat(final IRegExpMatcher<L> wellFormednessExpression,
                final Distance<L> one, final Distance<L> two) {
            if (one == Distance.ZERO) {
                return two;
            }
            if (one == Distance.INFINITE) {
                return Distance.INFINITE;
            }            
            if (two == Distance.ZERO) {
                return one;
            }
            if (two == Distance.INFINITE) {
                return Distance.INFINITE;
            }
            
            final List<L> mergedLabels = new ArrayList<>(0);
            mergedLabels.addAll(one.labels);
            mergedLabels.addAll(two.labels);           
            
//            java.util.function.Predicate<L> notLabelR = (Predicate<L>) HasLabel.labelEquals((L) Label.R).negate();
//            java.util.function.Predicate<L> notLabelD = (Predicate<L>) HasLabel.labelEquals((L) Label.D).negate();
            
            java.util.function.Predicate<L> notLabelR = label -> !label.equals(Label.R);
            java.util.function.Predicate<L> notLabelD = label -> !label.equals(Label.D);

            // @formatter:off
            final List<L> filteredLabels = mergedLabels.stream()
                    .filter(notLabelR)
                    .filter(notLabelD)
                    .collect(Collectors.toList());
            // @formatter:on            
            
            final IRegExpMatcher<L> matcherResult = wellFormednessExpression.match(filteredLabels);
                        
//            IRegExpMatcher<L> matcherResult = wellFormednessExpression;
//            for (L label : mergedLabels) {
//                if (!matcherResult.isAccepting()) {
//                    break;
//                }
//                matcherResult = matcherResult.match(label);
//            }
            
            if (matcherResult.isEmpty()) { // !matcherResult.isAccepting()
                return INFINITE;
            } else {
                return new Distance<>(mergedLabels);
            }
        }
        
        public Distance(L label) {
            this.labels = Collections.singletonList(label);
        }
        
        public Distance(List<L> labels) {
            this.labels = labels;
        }        

//        public Distance<L> concat(Distance<L> that) {
//
//            if (longValue > Integer.MAX_VALUE) {
//                return INFINITE;
//            } else {
//                final List<L> mergedLabels = new ArrayList<>(0);
//                mergedLabels.addAll(this.labels);
//                mergedLabels.addAll(that.labels);
//                
//                return Distance.of(longValue.intValue(), mergedLabels);
//            }
//        }
//
//        @Override
//        public int compareTo(Distance<L> that) {
//            return this.value - that.value;
//        }

        @Override
        public String toString() {
            if (this == INFINITE) {
                return "∞";
            } else {
                return labels.toString();
            }
        }

    }

    /*
     * TEMPORARY STATEFUL REPRESENTION OF ALL SHORTEST PATHS
     */
    private /* final */ Distance<L>[][] dist;
    private /* final */ int[][] next; 
    
    private /* final */ java.util.Map<Object, Integer> reverseIndex;
    private /* final */ java.util.Map<Integer, Object> forwardIndex;

    @Beta
    public final Set.Immutable<L> getLabels() {
        return labels;
    }

    @Beta
    public final PersistentScopeGraph<S, L, O> getScopeGraph() {
        return scopeGraph;
    }

    @Beta
    public final L getLabelD() {
        return labelD;
    }

    @Beta
    public final OpenCounter<S, L> getScopeCounter() {
        return scopeCounter;
    }

    @Beta
    public final IRelation<L> getOrdered() {
        return ordered;
    }

    @Beta
    public final IRelation<L> getUnordered() {
        return unordered;
    }

    @Beta
    public final IRegExpMatcher<L> getWf() {
        return wf;
    }

    // NOTE: never used in project
    @Deprecated
    @Override
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes();
    }

    // NOTE: all references could be duplicated to get rid of scope graph
    // reference
    @Override
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs();
    }

    @Override
    public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
        return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    @Override
    public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
        return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    }

    /**
     * Retrieves an environment builder for for a relation of labels.
     */
    public EnvironmentBuilder<S, L, O> getEnvironmentBuilder(final IRelation<L> lt) {
        return environmentBuilderCache.computeIfAbsent(lt, key -> EnvironmentBuilders.stage(key, labels));
    }

    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initTransients();
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(final O reference) {
        final int u = reverseIndex.get(reference);
        final Distance<L>[] reachableTargets = dist[u];
       
        final Set.Immutable<Integer> candidates = scopeGraph.declarationEdgeStream()
                .filter(slo -> slo.occurrence().getName().equals(reference.getName()))
                .map(tuple -> tuple.occurrence()).mapToInt(reverseIndex::get).boxed()
                .collect(CapsuleCollectors.toSet());
                
        // TODO: use configurable comparators based resolution, reachability and ..
        final Comparator<Distance<L>> comparator = new PathComparator<>(ordered);        
        
        // @formatter:off
        final OptionalInt declarationIndexAtMinimalCost = IntStream
                .range(0, reachableTargets.length)
                .filter(candidates::contains)
                .reduce((i, j) -> comparator.compare(reachableTargets[i], reachableTargets[j]) < 0 ? i : j);
        // @formatter:on              
            
        // TODO: support multiple paths
        if (declarationIndexAtMinimalCost.isPresent()) {
            @SuppressWarnings("unchecked")
            final O declaration = (O) forwardIndex.get(declarationIndexAtMinimalCost.getAsInt());
            
            final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> result = path(reference,
                    declaration).map(path -> {
                        final Set.Immutable<IResolutionPath<S, L, O>> paths = Set.Immutable.of(path);

                        final Set.Immutable<String> messages = Set.Immutable.of(path.getDeclaration().getIndex().getResource());

                        return ImmutableTuple2.of(paths, messages);
                    });
            
            return result;
        }

        return Optional.empty();
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

    @Override
    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        // TODO Auto-generated method stub
        return Optional.empty();
    }

}
