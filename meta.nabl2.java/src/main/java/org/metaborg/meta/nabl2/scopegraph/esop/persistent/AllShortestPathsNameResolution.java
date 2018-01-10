package org.metaborg.meta.nabl2.scopegraph.esop.persistent;

import static org.metaborg.meta.nabl2.util.tuples.HasOccurrence.occurrenceEquals;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.regexp.IRegExpMatcher;
import org.metaborg.meta.nabl2.regexp.RegExpMatcher;
import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.terms.Relation;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IResolutionParameters;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.OccurrenceLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.metaborg.util.functions.Predicate2;

import com.google.common.annotations.Beta;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.collection.AbstractSpecialisedImmutableMap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopNameResolution.Immutable<S, L, O>, java.io.Serializable {

    private static final long serialVersionUID = 42L;

    private static final boolean DEBUG = false;

    private final IEsopScopeGraph<S, L, O, V> scopeGraph;

    private final Set.Immutable<L> labels;
    private final IRegExpMatcher<L> wf;

    @Deprecated
    private final L labelD;
    
    private final IRelation<L> ordered;
    private final IRelation<L> unordered;

    @Deprecated
    private final IResolutionParameters<L> resolutionParameters;

    @Deprecated
    private final Predicate2<S, L> isEdgeClosed;

    public AllShortestPathsNameResolution(IEsopScopeGraph<S, L, O, V> scopeGraph,
            IResolutionParameters<L> resolutionParameters, Predicate2<S, L> isEdgeClosed) {
//        // Helps to detect errors depending on mutability of the scope graphs.
//        if (scopeGraph instanceof IEsopScopeGraph.Transient) {
//            this.scopeGraph = ((IEsopScopeGraph.Transient) scopeGraph).freeze();
//        } else {
//            this.scopeGraph = scopeGraph;
//        }

        this.scopeGraph = scopeGraph;
        this.resolutionParameters = resolutionParameters;
        this.isEdgeClosed = isEdgeClosed;

        this.labels = Set.Immutable.<L>of().__insertAll(Sets.newHashSet(resolutionParameters.getLabels()));
        this.labelD = resolutionParameters.getLabelD();
        this.wf = RegExpMatcher.create(resolutionParameters.getPathWf());
        
        this.ordered = resolutionParameters.getSpecificityOrder();
        this.unordered = Relation.Immutable.of(RelationDescription.STRICT_PARTIAL_ORDER);
        assert ordered.getDescription().equals(RelationDescription.STRICT_PARTIAL_ORDER) : "Label specificity order must be a strict partial order";

        final Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImports = scopeGraph.requireImportEdgeStream()
                .collect(CapsuleCollectors.toSet());

        this.resolutionResult = initAllShortestPaths(unresolvedImports, Map.Immutable.of(), Map.Immutable.of(), Map.Immutable.of());
    }

    private static final <S extends IScope, L extends ILabel, O extends IOccurrence> ScopeLabelScope<S, L, O> toDirectEdge(
            ScopeLabelOccurrence<S, L, O> requireImportEdge, OccurrenceLabelScope<O, L, S> associatedScopeEdge) {
        // TODO: look at this constraint; fails currently
        // assert Objects.equals(requireImportEdge.label(),
        // associatedScopeEdge.label());

        return ImmutableScopeLabelScope.of(requireImportEdge.scope(), requireImportEdge.label(),
                associatedScopeEdge.scope());
    }

    private ShortestPathResult<S, L, O> initAllShortestPaths(
            final Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImports,
            final Map.Immutable<ScopeLabelOccurrence<S, L, O>, IResolutionPath<S, L, O>> resolvedImports,
            final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> substitutionEvidence,
            final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidSubstitutionEvidence) {

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
        
        int rsOffset = 0;
        int rsLength = rs.size();
        
        int dsOffset = rsOffset + rsLength;
        int dsLength = ds.size();
        
        int scopesOffset = dsOffset + dsLength;
        int scopesLength = scopes.size();
        
        IntPredicate isReference = i -> rsOffset <= i && i < rsOffset + rsLength;
        IntPredicate isDeclaration = i -> dsOffset <= i && i < dsOffset + dsLength;
        IntPredicate isScope = i -> scopesOffset <= i && i < scopesOffset + scopesLength;

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
        
        /*
         * Adding direct edges that resulted from previously resolved imports.
         */
        substitutionEvidence.keySet().forEach(sls -> {
            final int sIndex = reverseIndex.get(sls.sourceScope());
            final int tIndex = reverseIndex.get(sls.targetScope());

            dist[sIndex][tIndex] = Distance.of(sls.label());
            next[sIndex][tIndex] = tIndex;
        });
        

        // TODO: use configurable comparators based resolution, reachability and
        // ..
        final Comparator<Distance<L>> comparator = new PathComparator<>(ordered);

        for (int k = 0; k < nodes.size(); k++) {
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = 0; j < nodes.size(); j++) {
                    // if (i == j) break;                    
                    
                    final Distance<L> ij = dist[i][j];
                    final Distance<L> ik = dist[i][k];
                    final Distance<L> kj = dist[k][j];

                    /*
                     * Calculating the cost of the transitive edge `ikj`.
                     * Optimizing for cases that do not require default path
                     * concatenation (i.e., if costs are ZERO or INFINITE).
                     */
                    final Distance<L> ikj;
                    
                    if (ik == Distance.INFINITE || kj == Distance.INFINITE) {
                        ikj = Distance.INFINITE;
                    } else if (ik == Distance.ZERO && kj == Distance.ZERO) {
                        ikj = Distance.ZERO;
                    } else if (ik == Distance.ZERO) {
                        ikj = kj;
                    } else if (kj == Distance.ZERO) {
                        ikj = ik;
                    } else {
                        ikj = Distance.concat(wf, ik, kj);
                    }

                    if (comparator.compare(ij, ikj) == 0 && !ij.equals(ikj)) {
                        System.out.println("breakpoint");
                    }

                    if (ikj != Distance.INFINITE && (ij == Distance.INFINITE || comparator.compare(ij, ikj) > 0)) {
                        dist[i][j] = ikj;
                        next[i][j] = next[i][k];

//                        if (DEBUG) {
//                            final String lessString = String.format("%s < %s", ikj, ij);
//                            final String distString = String.format("dist: %s -> %s = %s", nodes.get(i),
//                                    nodes.get(j), ikj);
//                            final String nextString = String.format("next: %s -> %s = %s", nodes.get(i),
//                                    nodes.get(j), nodes.get(next[i][k]));
//
//                            System.out.println(lessString);
//                            System.out.println(distString);
//                            System.out.println(nextString);
//                            System.out.println();
//                        }
//                    } else {
//                        if (DEBUG) {
//                            if (!(ij == Distance.INFINITE || ikj == Distance.INFINITE)  && !(ij == Distance.ZERO || ikj == Distance.ZERO) && !Objects.equals(ij, ikj)) {
//                                final String moreThanString = String.format("%s >= \n%s", ikj, ij);
//
//                                System.out.println(moreThanString);
//                                System.out.println();
//                            }
//                        }
                    }
                }
            }
        }
        
        if (DEBUG) {
            if (dist.length > 0) {
                // print sub-matrix showing distances from references to declarations  
                printMatrix(dist, isReference, isDeclaration);

                // print sub-matrix showing distances between inner scopes
                printMatrix(dist, isScope, isScope);

                System.out.println();
            }
        }
        
        final ShortestPathResult<S, L, O> tmpResolutionResult = new ShortestPathResult<>(dist, next, reverseIndex,
                forwardIndex, unresolvedImports, resolvedImports, substitutionEvidence, invalidSubstitutionEvidence);

        final Set.Transient<ScopeLabelOccurrence<S, L, O>> __unresolvedImports = Set.Transient.of();
        final Map.Transient<ScopeLabelOccurrence<S, L, O>, IResolutionPath<S, L, O>> __resolvedImports = resolvedImports
                .asTransient();
        final Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> __substitutionEvidence = substitutionEvidence
                .asTransient();
        final Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> __invalidSubstitutionEvidence = invalidSubstitutionEvidence
                .asTransient();        

        boolean importRevised = false;        
        for (ScopeLabelOccurrence<S, L, O> oldResolvedImport : resolvedImports.keySet()) {
            final IResolutionPath<S, L, O> oldResolutionPath = resolvedImports.get(oldResolvedImport);
            final IResolutionPath<S, L, O> newResolutionPath = tryResolve(scopeGraph, tmpResolutionResult, comparator,
                    oldResolvedImport.occurrence()).get()._1().findFirst().get();
            
            if (!oldResolutionPath.equals(newResolutionPath)) {              
                final ScopeLabelScope<S, L, O> directEdge = resolvedImportPathToDirectEdge(scopeGraph, oldResolvedImport, newResolutionPath);

                if (invalidSubstitutionEvidence.containsKey(directEdge) && invalidSubstitutionEvidence.get(directEdge).equals(newResolutionPath)) {
                    // no update necessary: this path was previously seen, but is less precise than the other paths 
                    break;
                }
                
                importRevised = true;
                
                if (DEBUG) {
                    System.out.println(String.format("invalided [ %s ]\n   old path: %s\n   new path: %s",
                            oldResolvedImport, oldResolutionPath, newResolutionPath));
                }

                __resolvedImports.put(oldResolvedImport, newResolutionPath);
                __substitutionEvidence.put(directEdge, newResolutionPath);                
                
                /*  
                 * Removing invalidated edges form solution and adding it to log. 
                 */                
                Iterator<Entry<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>>> entryIterator = __substitutionEvidence.entryIterator();
                while (entryIterator.hasNext()) {
                    Entry<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> entry = entryIterator.next();
                    if (entry.getValue().equals(oldResolutionPath)) {
                        __substitutionEvidence.__remove(entry.getKey());
                        __invalidSubstitutionEvidence.__put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        
        if (DEBUG) {
            if (importRevised) {
                System.out.println();
            }
        }

        /*
         * TODO: do we have to abort early after revised imports or can we
         * continue resolving other imports?
         */       
        if (!importRevised) {
            for (ScopeLabelOccurrence<S, L, O> unresolvedImport : unresolvedImports) {
                final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> resolution = tryResolve(
                        scopeGraph, tmpResolutionResult, comparator, unresolvedImport.occurrence());

                if (resolution.isPresent()) {
                    // TODO support multiple paths
                    final Set.Immutable<IResolutionPath<S, L, O>> declarationPaths = resolution.get()._1();
                    assert declarationPaths.size() == 1;

                    final IResolutionPath<S, L, O> path = declarationPaths.findFirst().get();
                    __resolvedImports.put(unresolvedImport, path);

                    final ScopeLabelScope<S, L, O> directEdge = resolvedImportPathToDirectEdge(scopeGraph, unresolvedImport, path);
                    __substitutionEvidence.put(directEdge, path);
                } else {
                    __unresolvedImports.__insert(unresolvedImport);
                }
            }
        }

        final ShortestPathResult<S, L, O> resolutionResult = new ShortestPathResult<>(dist, next, reverseIndex,
                forwardIndex, __unresolvedImports.freeze(), __resolvedImports.freeze(),
                __substitutionEvidence.freeze(), __invalidSubstitutionEvidence.freeze());

        if (substitutionEvidence.equals(__substitutionEvidence) && invalidSubstitutionEvidence.equals(__invalidSubstitutionEvidence)) { // substitutionEvidence.size() == __substitutionEvidence.size()
            if (DEBUG) {            
                System.out.println("final");
                System.out.println();
                System.out.println();
                System.out.println();
            }
            resolutionResult.isFinal = true;
            return resolutionResult;
        } else {
            return initAllShortestPaths(resolutionResult.unresolvedImports, resolutionResult.resolvedImports,
                    resolutionResult.substitutionEvidence, resolutionResult.invalidSubstitutionEvidence);
        }
    }
    
    private static final <L extends ILabel> void printMatrix(final Distance<L>[][] dist) {
        printMatrix(dist, i -> true, i -> true);
    }

    private static final <L extends ILabel> void printMatrix(final Distance<L>[][] dist, IntPredicate rowFilter, IntPredicate colFilter) {
        final int dimensionX = (int) IntStream.range(0, dist.length).filter(rowFilter).count();
        final int dimensionY = (int) IntStream.range(0, dist.length).filter(colFilter).count();

        if (dimensionX == 0 || dimensionY == 0) return;
        
        final int[] rowIDs = IntStream.range(0, dist.length).filter(rowFilter).toArray();
        final int[] colIDs = IntStream.range(0, dist.length).filter(colFilter).toArray();
        
        final int maxLength = IntStream.of(rowIDs)
                .mapToObj(rowId -> IntStream.of(colIDs).mapToObj(colId -> dist[rowId][colId]))
                .flatMap(stream -> stream)
                .map(Object::toString)
                .mapToInt(String::length)
                .max()
                .getAsInt();
        
        Function<Object, String> formatter = distance -> String.format("%" + maxLength + "s", distance);
        
        final String rowHead = "|  ";
        final String rowTail = "  |";
        
        final String sepHead = "---";
        final String sepTail = "---";
        
        final String columnFill = 
                IntStream.range(0, maxLength)
                    .mapToObj(position -> "-")
                    .reduce(String::concat).get();
        
        final String rowSeparator = 
                IntStream.range(0, dimensionY + 1)
                    .mapToObj(position -> columnFill)
                    .collect(Collectors.joining("---"));
        
        final String columnHeader = 
                Stream.concat(Stream.of(""), IntStream.of(colIDs).mapToObj(Integer::valueOf))
                    .map(formatter)
                    .collect(Collectors.joining(" | "));

        System.out.println(sepHead + rowSeparator + sepTail);
        System.out.println(rowHead + columnHeader + rowTail);
        System.out.println(sepHead + rowSeparator + sepTail);
        
        IntStream.of(rowIDs)
            .mapToObj(rowId -> Stream.concat(Stream.of(rowId), IntStream.of(colIDs).mapToObj(colId -> dist[rowId][colId])).map(formatter).collect(Collectors.joining(" | ")))
            .map(rowString -> rowHead + rowString + rowTail)
            .forEach(System.out::println);
        
        System.out.println(sepHead + rowSeparator + sepTail);        
    }
    
    private static <S extends IScope, L extends ILabel, O extends IOccurrence, V> ScopeLabelScope<S, L, O> resolvedImportPathToDirectEdge(
            final IEsopScopeGraph<S, L, O, V> scopeGraph, final ScopeLabelOccurrence<S, L, O> resolvedImport,
            final IResolutionPath<S, L, O> resolvedImportPath) {
        // @formatter:off
        final Set.Immutable<OccurrenceLabelScope<O, L, S>> associatedScopeEdges = scopeGraph.associatedScopeEdgeStream()
                .filter(occurrenceEquals(resolvedImportPath.getDeclaration())).collect(CapsuleCollectors.toSet());
                                
        final OccurrenceLabelScope<O, L, S> associatedScopeEdge = associatedScopeEdges.findFirst().get();
        // @formatter:on

        final ScopeLabelScope<S, L, O> directEdge = toDirectEdge(resolvedImport, associatedScopeEdge);
        return directEdge;
    }

    /*
     * Reconstruct path from all-shortest-paths matrix.
     */
    private static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>> path(
            final ShortestPathResult<S, L, O> resolutionResult, final O reference, final O declaration) {

        final int u = resolutionResult.reverseIndex.get(reference);

        int j = u;
        final int k = resolutionResult.reverseIndex.get(declaration);

        /*
         * if next[u][v] = null then return [] path = [u] while u ≠ v u ←
         * next[u][v] path.append(u) return path
         */

        final List<Object> trace = new ArrayList<>(0);

        if (resolutionResult.next[j][k] == -1) {
            return Optional.empty();
        } else {
            trace.add(resolutionResult.forwardIndex.get(j));

            // TODO: check second clause
            while (j != k && resolutionResult.next[j][k] != -1) {
                j = resolutionResult.next[j][k];
                trace.add(resolutionResult.forwardIndex.get(j));
            }
        }

        // if (DEBUG) {
        // final List<?> __states = trace.stream().collect(Collectors.toList());
        // final List<L> __labels =
        // resolutionResult.dist[u][k].labels.stream().collect(Collectors.toList());
        // System.out.println("breakpoint");
        // }

        // final List<S> states = trace.stream().skip(1).limit(trace.size() -
        // 2).map(scope -> (S) scope)
        // .collect(Collectors.toList());

        final List<?> candidateStates = trace.stream().skip(1).limit(trace.size() - 2).collect(Collectors.toList());

        // java.util.function.Predicate<L> notLabelR = label ->
        // !label.equals(Label.R);
        // java.util.function.Predicate<L> notLabelD = label ->
        // !label.equals(Label.D);
        //
        // final List<?> candidateStates = trace.stream()
        // .filter(node -> IScope.class.isInstance(node))
        // .collect(Collectors.toList());

        if (DEBUG) {
            if (!candidateStates.stream().allMatch(IScope.class::isInstance)) {
                final List<?> __states = trace.stream().collect(Collectors.toList());
                final List<L> __labels = resolutionResult.dist[u][k].labels.stream().collect(Collectors.toList());
                System.out.println("breakpoint");
            }
        }

        if (!candidateStates.stream().allMatch(IScope.class::isInstance)) {
            // path is invalid because it violates Scope -> Scope constraints
            return Optional.empty();
        }

        final List<S> states = candidateStates.stream().map(scope -> (S) scope).collect(Collectors.toList());
        final List<L> labels = resolutionResult.dist[u][k].labels.stream().limit(states.size())
                .collect(Collectors.toList());

        final IScopePath<S, L, O> pathStart = Paths.empty(states.get(0));
        final IScopePath<S, L, O> pathMiddle = IntStream.range(1, states.size())
                .mapToObj(i -> ImmutableTuple2.of(labels.get(i), states.get(i)))
                .reduce(pathStart, (pathIntermediate, tuple) -> {
                    final ScopeLabelScope<S, L, O> query = ImmutableScopeLabelScope.of(pathIntermediate.getTarget(),
                            tuple._1(), tuple._2());

                    if (resolutionResult.substitutionEvidence.containsKey(query)) {
                        final IResolutionPath<S, L, O> importPath = resolutionResult.substitutionEvidence.get(query);
                        return Paths
                                .append(pathIntermediate,
                                        Paths.named(pathIntermediate.getTarget(), tuple._1(), importPath, tuple._2()))
                                .get();
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

            final boolean isValidOne = Distance.isValid(o1);
            final boolean isValidTwo = Distance.isValid(o2);

            if (!isValidOne && !isValidTwo) {
                return 0;
            }
            if (!isValidOne) {
                return +1;
            }
            if (!isValidTwo) {
                return +1;
            }

            int commonLength = Math.min(o1.labels.size(), o2.labels.size());

            final boolean isAmbiguous = IntStream.range(0, commonLength).map(index -> {
                final L l1 = o1.labels.get(index);
                final L l2 = o2.labels.get(index);
                return !Objects.equals(l1, l2) && !labelOrder.smaller(l2).contains(l1)
                        && !labelOrder.larger(l2).contains(l1) ? 1 : 0;
            }).sum() == 0 ? false : true;

            if (isAmbiguous) {
                // incomparable or ambiguity respectively
                return 0;
            }

            final OptionalInt commonComparisonResult = IntStream.range(0, commonLength).map(index -> {
                L l1 = o1.labels.get(index);
                L l2 = o2.labels.get(index);

                if (Objects.equals(l1, l2)) {
                    return 0;
                }

                if (labelOrder.smaller(l2).contains(l1)) {
                    return -1;
                } else if (labelOrder.larger(l2).contains(l1)) {
                    return +1;
                } else {
                    throw new IllegalStateException("Incomparable or ambiguous labels must be handled beforehand.");
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

    public static class Distance<L extends ILabel> implements java.io.Serializable {

        private static final long serialVersionUID = 42L;

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance ZERO = new Distance(Collections.EMPTY_LIST);

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public static final Distance INFINITE = new Distance(Collections.EMPTY_LIST);

        private final List<L> labels;

        public static final <L extends ILabel> Distance<L> of(L label) {
            return new Distance<L>(label);
        }
        
        public Distance(L label) {
            this.labels = Collections.singletonList(label);
        }

        public Distance(List<L> labels) {
            this.labels = labels;
        }        

        @SuppressWarnings("unchecked")
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

            final List<L> mergedLabels = new ArrayList<>(one.labels.size() + two.labels.size());
            mergedLabels.addAll(one.labels);
            mergedLabels.addAll(two.labels);
            assert mergedLabels.size() >= 2;
            
            final Distance<L> concatenation = new Distance<>(mergedLabels);

            if (!Distance.isValid(concatenation)) {
                return INFINITE;
            }
            
            // @formatter:off
            final List<L> filteredLabels = mergedLabels.stream()
                    .filter(label -> !label.equals(Label.R))
                    .filter(label -> !label.equals(Label.D))
                    .collect(Collectors.toList());
            // @formatter:on            

            final IRegExpMatcher<L> matcherResult = wellFormednessExpression.match(filteredLabels);

            if (matcherResult.isEmpty()) {
                return INFINITE;
            } else {
                return concatenation;
            }
        }

        /*
         * Checks that paths contain at most one reference label and at most one
         * declaration label. If present, a declaration occur at the first
         * position, and a declaration at the last position.
         */
        public static final <L extends ILabel> boolean isValid(Distance<L> distance) {
            List<L> mergedLabels = distance.labels;

            if (mergedLabels.size() == 1) {
                return true;
            }

            long countOfLabelR = mergedLabels.stream().filter(label -> label.equals(Label.R)).count();
            long countOfLabelD = mergedLabels.stream().filter(label -> label.equals(Label.D)).count();

            if (countOfLabelR == 1 && !mergedLabels.get(0).equals(Label.R)) {
                return false;
            }

            if (countOfLabelD == 1 && !mergedLabels.get(mergedLabels.size() - 1).equals(Label.D)) {
                return false;
            }

            return true;
        }        
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((labels == null) ? 0 : labels.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Distance other = (Distance) obj;
            if (labels == null) {
                if (other.labels != null)
                    return false;
            } else if (!labels.equals(other.labels))
                return false;
            return true;
        }

        @Override
        public String toString() {
            if (this == INFINITE) {
                return "∞";
            } else if (this == ZERO) {
                return "∅";
            } else {
                return labels.toString();
            }
        }

    }

    public static class ShortestPathResult<S extends IScope, L extends ILabel, O extends IOccurrence> {
        public final Distance<L>[][] dist;
        public final int[][] next;

        public final java.util.Map<Object, Integer> reverseIndex;
        public final java.util.Map<Integer, Object> forwardIndex;

        public final Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImports;
        public final Map.Immutable<ScopeLabelOccurrence<S, L, O>, IResolutionPath<S, L, O>> resolvedImports;
        public final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> substitutionEvidence;
        public final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidSubstitutionEvidence;

        public boolean isFinal = false;
        
        public ShortestPathResult(final Distance<L>[][] dist, final int[][] next,
                final java.util.Map<Object, Integer> reverseIndex, final java.util.Map<Integer, Object> forwardIndex,
                final Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImports,
                final Map.Immutable<ScopeLabelOccurrence<S, L, O>, IResolutionPath<S, L, O>> resolvedImports,
                final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> substitutionEvidence,
                final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidSubstitutionEvidence) {
            this.dist = dist;
            this.next = next;

            this.reverseIndex = reverseIndex;
            this.forwardIndex = forwardIndex;

            this.unresolvedImports = unresolvedImports;
            this.resolvedImports = resolvedImports;
            this.substitutionEvidence = substitutionEvidence;
            this.invalidSubstitutionEvidence = invalidSubstitutionEvidence;
        }
    }

    /*
     * TEMPORARY STATEFUL REPRESENTION OF ALL SHORTEST PATHS
     */
    private final ShortestPathResult<S, L, O> resolutionResult;

    @Beta
    public final Set.Immutable<L> getLabels() {
        return labels;
    }

    @Beta
    @Deprecated
    @Override
    public IResolutionParameters<L> getResolutionParameters() {
        return resolutionParameters;
    }

    @Beta
    @Override
    public final IEsopScopeGraph<S, L, O, V> getScopeGraph() {
        return scopeGraph;
    }

    @Override
    public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return getAllRefs().stream().filter(reference -> this.resolve(reference).isPresent())
                .map(reference -> AbstractSpecialisedImmutableMap.entryOf(reference, this.resolve(reference).get()))
                .collect(CapsuleCollectors.toSet());
    }

    @Deprecated
    @Override
    public boolean isEdgeClosed(S scope, L label) {
        return isEdgeClosed.test(scope, label);
    }

    @Beta
    @Deprecated
    public final L getLabelD() {
        return labelD;
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
    public Set.Immutable<S> getAllScopes() {
        return scopeGraph.getAllScopes().stream().collect(CapsuleCollectors.toSet());
    }

    // NOTE: all references could be duplicated to get rid of scope graph
    // reference
    public Set.Immutable<O> getAllRefs() {
        return scopeGraph.getAllRefs().stream().collect(CapsuleCollectors.toSet());
    }

    // public Set.Immutable<IResolutionPath<S, L, O>> resolve(O ref) {
    // return tryResolve(ref).map(Tuple2::_1).orElse(Set.Immutable.of());
    // }
    //
    // public Set.Immutable<IDeclPath<S, L, O>> visible(S scope) {
    // return tryVisible(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    // }
    //
    // public Set.Immutable<IDeclPath<S, L, O>> reachable(S scope) {
    // return tryReachable(scope).map(Tuple2::_1).orElse(Set.Immutable.of());
    // }

    @Override
    public Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
        return tryResolve(ref).map(Tuple2::_1);
    }

    @Override
    public Optional<Set.Immutable<O>> visible(S scope) {
        // return tryVisible(scope).map(Tuple2::_1);

        final Optional<Set.Immutable<IDeclPath<S, L, O>>> result = tryVisible(scope).map(Tuple2::_1);

        if (result.isPresent()) {
            return Optional
                    .of(result.get().stream().map(path -> path.getDeclaration()).collect(CapsuleCollectors.toSet()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Set.Immutable<O>> reachable(S scope) {
        // return tryReachable(scope).map(Tuple2::_1);

        final Optional<Set.Immutable<IDeclPath<S, L, O>>> result = tryReachable(scope).map(Tuple2::_1);

        if (result.isPresent()) {
            return Optional
                    .of(result.get().stream().map(path -> path.getDeclaration()).collect(CapsuleCollectors.toSet()));
        } else {
            return Optional.empty();
        }
    }

    // serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // initTransients();
    }

    private static <S extends IScope, L extends ILabel, O extends IOccurrence, V> Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(
            final IEsopScopeGraph<S, L, O, V> scopeGraph, final ShortestPathResult<S, L, O> resolutionResult,
            final Comparator<Distance<L>> comparator, final O reference) {
        final int u = resolutionResult.reverseIndex.get(reference);
        final Distance<L>[] visibleTargets = resolutionResult.dist[u];

        final Optional<ScopeLabelOccurrence<S, L, O>> sloReference = resolutionResult.resolvedImports.keySet().stream().filter(slo -> slo.occurrence().equals(reference)).findFirst(); 
        
        if (resolutionResult.isFinal && sloReference.isPresent()) {
            final IResolutionPath<S, L, O> path = resolutionResult.resolvedImports.get(sloReference.get());            
            return Optional.of(ImmutableTuple2.of(Set.Immutable.of(path), Set.Immutable.of()));
        }
        
//        final Set.Immutable<IResolutionPath<S, L, O>> invalidImportResolutionPaths = resolutionResult.invalidSubstitutionEvidence.values().stream()
//            .filter(path -> path.getReference().equals(reference))
//            .collect(CapsuleCollectors.toSet());
//        
//        if (!invalidImportResolutionPaths.isEmpty()) {
//            return Optional.of(ImmutableTuple2.of(invalidImportResolutionPaths, Set.Immutable.of()));
//        }
        
        final Set.Immutable<O> invalidImportResolutionDeclarations = resolutionResult.invalidSubstitutionEvidence.values().stream()
                .filter(path -> path.getReference().equals(reference))
                .map(path -> path.getDeclaration())
                .collect(CapsuleCollectors.toSet());          
        
//        final Set.Immutable<IResolutionPath<S, L, O>> importResolutionPaths = resolutionResult.substitutionEvidence.values().stream()
//                .filter(path -> path.getReference().equals(reference))
//                .collect(CapsuleCollectors.toSet());      
//        
//        if (!importResolutionPaths.isEmpty()) {
//            return Optional.of(ImmutableTuple2.of(importResolutionPaths, Set.Immutable.of()));
//        }
                
        final Set.Immutable<O> importResolutionDeclarations = resolutionResult.substitutionEvidence.values().stream()
                .filter(path -> path.getReference().equals(reference))
                .map(path -> path.getDeclaration())
                .collect(CapsuleCollectors.toSet());  
        
        final Set.Immutable<Integer> candidates = scopeGraph.declarationEdgeStream()
                .filter(slo -> slo.occurrence().getName().equals(reference.getName())).map(tuple -> tuple.occurrence())
                .mapToInt(resolutionResult.reverseIndex::get).boxed().collect(CapsuleCollectors.toSet());

        /*
         * TODO: represent minimal cost as absolute number (instead of a
         * concrete list of labels) to accommodate for ambiguities.
         */
        // @formatter:off
        final OptionalInt declarationIndexAtMinimalCost = IntStream
                .range(0, visibleTargets.length)
                .filter(candidates::contains)
                .reduce((i, j) -> comparator.compare(visibleTargets[i], visibleTargets[j]) < 0 ? i : j);
        // @formatter:on

        /*
         * NOTE: minimalDistance is an example instance of at minimal cost.
         * However, there could be ambiguous instance with the same cost.
         */
        final Distance<L> minimalDistanceExample;

        if (declarationIndexAtMinimalCost.isPresent()) {
            minimalDistanceExample = visibleTargets[declarationIndexAtMinimalCost.getAsInt()];
        } else {
            minimalDistanceExample = Distance.INFINITE;
        }

        // check if ambiguous
        if (declarationIndexAtMinimalCost.isPresent() && !minimalDistanceExample.equals(Distance.INFINITE)) {
            long uniqueSolutionCount = IntStream.range(0, visibleTargets.length).filter(candidates::contains)
                    .mapToObj(index -> visibleTargets[index]).distinct().count();

            final Set.Immutable<Integer> indices = IntStream.range(0, visibleTargets.length)
                    .filter(candidates::contains)
                    .filter(index -> comparator.compare(visibleTargets[index], minimalDistanceExample) == 0)
                    .mapToObj(Integer::valueOf).collect(CapsuleCollectors.toSet());

            long minimalCostSolutionCount = indices.size();

            final Set.Immutable<O> declarations = indices.stream()
                    .map(index -> (O) resolutionResult.forwardIndex.get(index)).collect(CapsuleCollectors.toSet());

            long declarationsCount = declarations.size();

            if (minimalCostSolutionCount > 1) {
                // ambiguity detected
                return Optional.empty();
            }
        }

        // TODO: support multiple paths
        if (declarationIndexAtMinimalCost.isPresent() && !minimalDistanceExample.equals(Distance.INFINITE)) {
            @SuppressWarnings("unchecked")
            final O declaration = (O) resolutionResult.forwardIndex.get(declarationIndexAtMinimalCost.getAsInt());

            final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> result = path(
                    resolutionResult, reference, declaration).map(path -> {
                        final Set.Immutable<IResolutionPath<S, L, O>> paths = Set.Immutable.of(path);

                        final Set.Immutable<String> messages = Set.Immutable
                                .of(path.getDeclaration().getIndex().getResource());

                        return ImmutableTuple2.of(paths, messages);
                    });

            return result;
        }

        return Optional.empty();
    }

    public Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(
            final O reference) {
        final Comparator<Distance<L>> comparator = new PathComparator<>(ordered);
        return tryResolve(scopeGraph, resolutionResult, comparator, reference);
    }

    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryVisible(S scope) {
        // TODO: vastly improve performance and general architecture of this
        // method!!!

        final int u = resolutionResult.reverseIndex.get(scope);
        final Distance<L>[] visibleTargets = resolutionResult.dist[u];

        final IFunction<O, S> refs = scopeGraph.getRefs();

        // @formatter:off
        final Set.Immutable<IDeclPath<S, L, O>> visibleDeclarations = IntStream
                .range(0, visibleTargets.length)
//                .filter(index -> visibleTargets[index] != Distance.ZERO)
                .filter(index -> visibleTargets[index] != Distance.INFINITE)
                .mapToObj(index -> resolutionResult.forwardIndex.get(index))
                .filter(IOccurrence.class::isInstance)
                .map(IOccurrence.class::cast)
                .map(o -> (O) o)
                .filter(refs::containsKey)
                .map(o -> Paths.<S, L, O>decl(Paths.empty(refs.get(o).get()), o))
                .collect(CapsuleCollectors.toSet());
        // @formatter:on           

        final Set.Immutable<String> messages = Set.Immutable.of("::");

        return Optional.of(ImmutableTuple2.of(visibleDeclarations, messages));
    }

    public Optional<Tuple2<Set.Immutable<IDeclPath<S, L, O>>, Set.Immutable<String>>> tryReachable(S scope) {
        final Comparator<Distance<L>> comparator = new PathComparator<>(unordered);
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public java.util.Set<O> getResolvedRefs() {
        return getAllRefs().stream().filter(reference -> this.resolve(reference).isPresent())
                .collect(CapsuleCollectors.toSet());
    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution.Transient<S, L, O> melt(
            IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed) {
        return new TransientAllShortestPathsNameResolution<>(this, isEdgeClosed);
    }

}

class TransientAllShortestPathsNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence, V>
        implements IEsopNameResolution.Transient<S, L, O> {

    private IEsopNameResolution.Immutable<S, L, O> solution;

    @Deprecated
    private final Predicate2<S, L> isEdgeClosed;

    TransientAllShortestPathsNameResolution(final IEsopNameResolution.Immutable<S, L, O> solution,
            final Predicate2<S, L> isEdgeClosed) {
        this.solution = solution;
        this.isEdgeClosed = isEdgeClosed;
    }

    @Beta
    @Override
    public IResolutionParameters<L> getResolutionParameters() {
        return solution.getResolutionParameters();
    }

    @Beta
    @Override
    public IEsopScopeGraph<S, L, O, ?> getScopeGraph() {
        return solution.getScopeGraph();
    }

    @Override
    public boolean isEdgeClosed(S scope, L label) {
        return isEdgeClosed.test(scope, label);
    }

    @Override
    public java.util.Set<O> getResolvedRefs() {
        return solution.getResolvedRefs();
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>> resolve(O ref) {
        return solution.resolve(ref);
    }

    @Override
    public void resolveAll(Iterable<? extends O> refs) {
        // no-op: all-shortest-paths algorithm does it anyways

        /**
         * Force re-resolution due to mutable updates. Hack necessary due to
         * assumptions in {@link NameResolutionComponent#update()}.
         */
        IEsopNameResolution.Immutable<S, L, O> mergedNameResolution = IEsopNameResolution
                .builder(this.getResolutionParameters(), this.getScopeGraph(), isEdgeClosed).freeze();

        this.solution = mergedNameResolution;
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> visible(S scope) {
        return solution.visible(scope);
    }

    @Override
    public Optional<io.usethesource.capsule.Set.Immutable<O>> reachable(S scope) {
        return solution.reachable(scope);
    }

    @Override
    public java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> resolutionEntries() {
        return solution.resolutionEntries();
    }

    @Override
    public boolean addAll(IEsopNameResolution<S, L, O> that) {
        // throw new UnsupportedOperationException("Not yet implemented.");

        IEsopScopeGraph<S, L, O, V> graph1 = (IEsopScopeGraph<S, L, O, V>) this.getScopeGraph();
        IEsopScopeGraph<S, L, O, V> graph2 = (IEsopScopeGraph<S, L, O, V>) that.getScopeGraph();

        java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> res1 = this
                .resolutionEntries();
        java.util.Set<Entry<O, io.usethesource.capsule.Set.Immutable<IResolutionPath<S, L, O>>>> res2 = that
                .resolutionEntries();
        boolean isModified = !res1.equals(res2);

        IEsopScopeGraph.Transient<S, L, O, V> builder = IEsopScopeGraph.builder();
        builder.addAll(graph1);
        builder.addAll(graph2);
        IEsopScopeGraph.Immutable<S, L, O, ?> mergedGraphs = builder.freeze();

        assert Objects.equals(this.getResolutionParameters(), that.getResolutionParameters());

        IResolutionParameters<L> mergedResolutionParameters = this.getResolutionParameters();
        // Predicate2<S, L> mergedEdgeClosedPredicate = (s, l) -> true;

        IEsopNameResolution.Immutable<S, L, O> mergedNameResolution = IEsopNameResolution
                .builder(mergedResolutionParameters, mergedGraphs, isEdgeClosed).freeze();

        this.solution = mergedNameResolution;

        return isModified;
    }

    @Override
    public org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution.Immutable<S, L, O> freeze() {
        return solution;
    }

}
