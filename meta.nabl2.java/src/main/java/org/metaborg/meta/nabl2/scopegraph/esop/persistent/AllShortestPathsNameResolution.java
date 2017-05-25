package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasOccurrence.occurrenceEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IEsopNameResolution<S, L, O>, Serializable {

    private static final long serialVersionUID = 42L;

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

        final Set.Immutable<O> importToUnfold = scopeGraph.requireImportEdgeStream().map(tuple -> tuple.occurrence())
                .collect(CapsuleCollectors.toSet());
        
//        final Set.Immutable<O> importToUnfold = scopeGraph.associatedScopeEdgeStream().map(tuple -> tuple.occurrence())
//                .collect(CapsuleCollectors.toSet());
        
        // final Set.Immutable<ScopeLabelOccurrence<S, L, O>> importToUnfoldMap
        // = scopeGraph.importReferencesStream()
        // .collect(CapsuleCollectors.toSet());

        initAllShortestPaths(importToUnfold, Map.Immutable.of());
    }

    private void initTransients() {
        this.environmentBuilderCache = Maps.newHashMap();
    }

    private void initAllShortestPaths(final Set.Immutable<O> importToUnfold, final Map.Immutable<O, S> importSubstitutionMap) {
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

        
        importSubstitutionMap.forEach((o, t) -> {
            // TODO: improve labeling !!!
            final Distance<L> distance = new Distance<>(10, (L) Label.Q);
            
            final S s = scopeGraph.requireImportEdgeStream().filter(occurrenceEquals(o)).map(tuple -> tuple.scope())
                    .findAny().get();            
            
            final int sIndex = reverseIndex.get(s);
            final int tIndex = reverseIndex.get(t);            
            
            dist[sIndex][tIndex] = distance;
            next[sIndex][tIndex] = tIndex;
        });
        
        /*
         * Assigning the weight of the edge (u,v).
         * 
         * for each edge (u,v) { dist[u][v] <- w(u,v) }
         */
        scopeGraph.sourceEdgeStream().forEach(slo -> {
            final O o = slo.occurrence();
            final L l = slo.label();
            final S s = slo.scope();

            final Distance<L> distance;
            switch (l.toString()) {
            case "I()":
                distance = new Distance<>(10, l);
                break;
            case "R()":
                distance = new Distance<>(1, l); 
                break;
            default:
                throw new IllegalStateException(l.toString());
            }

            final int oIndex = reverseIndex.get(o);
            final int sIndex = reverseIndex.get(s);
            
            dist[oIndex][sIndex] = distance;
            next[oIndex][sIndex] = sIndex;
        });
        scopeGraph.middleEdgeStream().forEach(sls -> {
            final S s = sls.sourceScope();
            final L l = sls.label();
            final S t = sls.targetScope();

            final Distance<L> distance;
            switch (l.toString()) {
            case "P()":
                distance = new Distance<>(100, l);
                break;
            default:
                throw new IllegalStateException(l.toString());
            }

            final int sIndex = reverseIndex.get(s);
            final int tIndex = reverseIndex.get(t);
            
            dist[sIndex][tIndex] = distance;
            next[sIndex][tIndex] = tIndex;
        });
        scopeGraph.targetEdgeStream().forEach(slo -> {
            final S s = slo.scope();
            final L l = slo.label();
            final O o = slo.occurrence();

            final Distance<L> distance;
            switch (l.toString()) {
            case "I()":
                distance = new Distance<>(10, l);
                break;
            case "D()":
                distance = new Distance<>(1, l);
                break;
            default:
                throw new IllegalStateException(l.toString());
            }
            
            final int sIndex = reverseIndex.get(s);
            final int oIndex = reverseIndex.get(o);

            dist[sIndex][oIndex] = distance;
            next[sIndex][oIndex] = oIndex;
        });        

        for (int k = 0; k < nodes.size(); k++) {
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = 0; j < nodes.size(); j++) {
                    Distance<L> ij = dist[i][j];
                    Distance<L> ik = dist[i][k];
                    Distance<L> kj = dist[k][j];

                    Distance<L> ikj = ik.sum(kj);

                    if (ij.compareTo(ikj) > 0) {
                        dist[i][j] = ikj;
                        next[i][j] = next[i][k];
                        
                        if (forwardIndex.get(i) instanceof IOccurrence && forwardIndex.get(j) instanceof IOccurrence) {
                            String distString = String.format("dist: %s -> %s = %s", nodes.get(i), nodes.get(j), ikj);
                            String nextString = String.format("next: %s -> %s = %s", nodes.get(i), nodes.get(j),
                                    nodes.get(next[i][k]));
                           
                            System.out.println(distString);
                            // System.out.println(nextString);
                            System.out.println();
                        }
                    }
                }
            }
        }

        this.dist = dist;
        this.next = next;
        
        this.nodes = nodes;

        this.forwardIndex = forwardIndex;
        this.reverseIndex = reverseIndex;
        
        final Set.Transient<O> importToUnfoldNew = Set.Transient.of();
        final Map.Transient<O, S> importSubstitutionMapNew = importSubstitutionMap.asTransient();        

        for (O o : importToUnfold) {
            if (tryResolve(o).isPresent()) {
                // System.out.println("Import resolved.");
                
                // TODO support multiple paths
                final Set.Immutable<IResolutionPath<S, L, O>> declarationPaths = resolve(o);
                
                final IResolutionPath<S, L, O> path = declarationPaths.findFirst().get();
                
//                final Set.Immutable<ScopeLabelOccurrence<S, L, O>> collect = scopeGraph.exportDeclarationsStream()
//                        .filter(occurrenceEquals(path.getDeclaration())).collect(CapsuleCollectors.toSet());                

                // @formatter:off
                final S associatedScope = scopeGraph.associatedScopeEdgeStream()
                        .filter(occurrenceEquals(path.getDeclaration()))
                        .map(tuple -> tuple.scope())
                        .findAny().get();
                // @formatter:on
                
                importSubstitutionMapNew.__put(o, associatedScope);
            } else {
                // System.out.println("Import not resolvable.");
                
                importToUnfoldNew.__insert(o);
            }               
        }
                
        if (importToUnfold.size() == importToUnfoldNew.size()) {
            return;
        } else {
            initAllShortestPaths(importToUnfoldNew.freeze(), importSubstitutionMapNew.freeze());
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
        
        /*
         * Paths.decl(path, declaration);
         * 
         * Paths.append(path, Paths.direct(path.getTarget(), label, nextScope));
         * 
         * Paths.named(path.getTarget(), label, importPath, nextScope);
         */
        
        final List<?> __states = trace.stream().skip(1).limit(trace.size() - 2).collect(Collectors.toList());
        final List<L> __labels = dist[u][k].labels.stream().limit(__states.size()).collect(Collectors.toList());        
        
        @SuppressWarnings("unchecked")
        final List<S> states = trace.stream().skip(1).limit(trace.size() - 2).map(scope -> (S) scope)
                .collect(Collectors.toList());
        final List<L> labels = dist[u][k].labels.stream().limit(states.size()).collect(Collectors.toList());

        final IScopePath<S, L, O> pathStart = Paths.empty(states.get(0));
        final IScopePath<S, L, O> pathMiddle = IntStream.range(1, states.size())
                .mapToObj(i -> ImmutableTuple2.of(labels.get(i), states.get(i)))
                .reduce(pathStart, (pathIntermediate, tuple) -> {
                    return Paths.append(pathIntermediate,
                            Paths.direct(pathIntermediate.getTarget(), tuple._1(), tuple._2())).get();
                }, (p1, p2) -> Paths.append(p1, p2).get());
            
        final Optional<IResolutionPath<S, L, O>> resolutionPath = Paths.resolve(reference, pathMiddle, declaration);
        return resolutionPath;
    }

    public static class Distance<L extends ILabel> implements Comparable<Distance<L>> {

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance ZERO = new Distance(0, Collections.EMPTY_LIST);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance INFINITE = new Distance(Integer.MAX_VALUE, Collections.EMPTY_LIST);

        private final int value;
        private final List<L> labels;

        public Distance(int value, L label) {
            this.value = value;
            this.labels = Collections.singletonList(label);
        }
        
        public Distance(int value, List<L> labels) {
            this.value = value;
            this.labels = labels;
        }        

        public Distance<L> sum(Distance<L> that) {
            Long longValue = (long) this.value + (long) that.value;

            if (longValue > Integer.MAX_VALUE) {
                return INFINITE;
            } else {
                final List<L> mergedLabels = new ArrayList<>(0);
                mergedLabels.addAll(this.labels);
                mergedLabels.addAll(that.labels);
                
                return new Distance<>(longValue.intValue(), mergedLabels);
            }
        }

        @Override
        public int compareTo(Distance<L> that) {
            return this.value - that.value;
        }

        @Override
        public String toString() {
            if (value == Integer.MAX_VALUE) {
                return "∞" + labels.toString();
            } else {
                return Integer.toString(value) + labels.toString();
            }
        }

    }

    /*
     * private final IRelation3<S, L, O> declarations; private final
     * IRelation3<S, L, O> references;
     */
    // = new O[scopeGraph][];
    // private final O[][] referencesToDeclarations_P;
    // private final O[][] referencesToDeclarations_I;
    // private final O[][] referencesToDeclarations_D;

    private /* final */ Distance<L>[][] dist;
    private /* final */ int[][] next;

    private /* final */ List<Object> nodes; 
    
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
                
        // @formatter:off
        final OptionalInt declarationIndexAtMinimalCost = IntStream
                .range(0, reachableTargets.length)
                .filter(candidates::contains)
                .reduce((i, j) -> reachableTargets[i].compareTo(reachableTargets[j]) < 0 ? i : j);
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
