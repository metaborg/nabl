package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
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
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopNameResolution;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.collections.IFunction;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
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
    private final AllShortestPathsResult<S, L, O> resolutionResult;

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

        this.resolutionResult = initAllShortestPaths(new AllShortestPathsParameters<>(unresolvedImports), AllShortestPathsResult.empty());
    }

    private AllShortestPathsResult<S, L, O> initAllShortestPaths(final AllShortestPathsParameters<S, L, O> resolutionParameters, final AllShortestPathsResult<S, L, O> previousSolution) {
        
                
        /**********************************************************************
         * Mapping graph vertices (references, declaration, and scopes) to integer numbers.
         **********************************************************************/
        
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

        
        /**********************************************************************
         * Predicates for pretty-printing matrices. 
         **********************************************************************/        
        
        int rsOffset = 0;
        int rsLength = rs.size();
        
        int dsOffset = rsOffset + rsLength;
        int dsLength = ds.size();
        
        int scopesOffset = dsOffset + dsLength;
        int scopesLength = scopes.size();
        
        IntPredicate isReference = i -> rsOffset <= i && i < rsOffset + rsLength;
        IntPredicate isDeclaration = i -> dsOffset <= i && i < dsOffset + dsLength;
        IntPredicate isScope = i -> scopesOffset <= i && i < scopesOffset + scopesLength;

        
        /**********************************************************************
         * Initalizing distance matrix and next matrix (for recovering paths). 
         **********************************************************************/        
        
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
        resolutionParameters.resolvedImports.values().stream().forEach(sls -> {
            final int sIndex = reverseIndex.get(sls.sourceScope());
            final int tIndex = reverseIndex.get(sls.targetScope());

            dist[sIndex][tIndex] = Distance.of(sls.label());
            next[sIndex][tIndex] = tIndex;
        });
        

        /**********************************************************************
         * Calculating shortest paths.
         **********************************************************************/
        
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


        /**********************************************************************
         * Result from running shortest path algorithm.
         **********************************************************************/        
        
        final AllShortestPathsResult<S, L, O> resolutionResult = new AllShortestPathsResult<>(dist, next, reverseIndex, forwardIndex, resolutionParameters);

        if (DEBUG) {
            if (dist.length > 0) {
                // print sub-matrix showing distances from references to declarations  
                resolutionResult.printMatrix(isReference, isDeclaration);

                // print sub-matrix showing distances between inner scopes
                resolutionResult.printMatrix(isScope, isScope);

                System.out.println();
            }
        }
        
                
        /**********************************************************************
         * Checking if imports were invalidated (e.g., do deal with import anomaly).
         **********************************************************************/
        
        final AllShortestPathsParametersBuilder<S, L, O> nextResolutionParametersBuilder = resolutionParameters.asTransient();
        
        boolean importRevised = false;        
        for (O importReference : resolutionParameters.resolvedImports.keySet()) {
                        
            final Set.Immutable<IResolutionPath<S, L, O>> newImportPaths = 
                    resolveToPaths(scopeGraph, resolutionResult, comparator, importReference).get();
            
            final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath = 
                    scopeGraph.joinImports(newImportPaths);
            
            final java.util.Set<ScopeLabelScope<S, L, O>> newDirectEdges = directEdgeToResolutionPath.keySet();
            final java.util.Set<ScopeLabelScope<S, L, O>> oldDirectEdges = resolutionParameters.resolvedImports.get(importReference);
                        
            if (!oldDirectEdges.equals(newDirectEdges)) {

                final Set.Immutable<ScopeLabelScope<S, L, O>> invalidatedDirectEdges = 
                        newDirectEdges.stream()
                            .filter(directEdge -> resolutionParameters.isImportEdgeInvalidated(importReference, directEdge))
                            .collect(CapsuleCollectors.toSet());
                
                if (invalidatedDirectEdges.isEmpty()) {
                    nextResolutionParametersBuilder.updateImport(importReference, directEdgeToResolutionPath);
                    importRevised = true;
            
                    if (DEBUG) {
                        System.out.println(String.format("invalided [ %s ]\n   old path: %s\n   new path: %s",
                                importReference, oldDirectEdges, newDirectEdges));
                    }                
                } else if (invalidatedDirectEdges.size() != newDirectEdges.size()) {
                    throw new IllegalStateException("Assumed that either all edges or none become invalidated.");
                }
            }
        }
        
        if (DEBUG) {
            if (importRevised) {
                System.out.println();
            }
        }

        
        /**********************************************************************
         * Try to resolve imports that were reachable in this round.
         **********************************************************************/
        
        /*
         * TODO: do we have to abort early after revised imports or can we continue resolving other imports?
         */
        if (!importRevised) {
            for (O importReference : resolutionParameters.unresolvedImportReferences()) {                

                final Optional<Set.Immutable<IResolutionPath<S, L, O>>> optionalImportPaths = 
                        resolveToPaths(scopeGraph, resolutionResult, comparator, importReference);
                
                if (optionalImportPaths.isPresent()) {                
                    final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath = 
                            scopeGraph.joinImports(optionalImportPaths.get());
                    
                    nextResolutionParametersBuilder.resolveImport(importReference, directEdgeToResolutionPath);
                }
            }
        }
        
        
        /**********************************************************************
         * Determining if all resolvable references and imports were resolved.
         **********************************************************************/        
        
        final AllShortestPathsParameters<S, L, O> nextResolutionParameters = nextResolutionParametersBuilder.freeze();
        
        boolean isFixpointReached = 
                resolutionParameters.resolvedImports.size() == nextResolutionParameters.resolvedImports.size()
                        && resolutionParameters.invalidImports.size() == nextResolutionParameters.invalidImports.size();

        if (isFixpointReached) {
            if (DEBUG) {            
                System.out.println("final");
                System.out.println();
                System.out.println();
                System.out.println();
            }
            resolutionResult.isFinal = true;
            return resolutionResult;
        } else {
            return initAllShortestPaths(nextResolutionParameters, resolutionResult);
        }
    }

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
        final Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> resolution = tryResolve(ref);
        return resolution.map(Tuple2::_1);
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
    }

    private static <S extends IScope, L extends ILabel, O extends IOccurrence, V> Optional<Set.Immutable<IResolutionPath<S, L, O>>> resolveToPaths(final IEsopScopeGraph<S, L, O, V> scopeGraph, final AllShortestPathsResult<S, L, O> resolutionResult, final Comparator<Distance<L>> comparator, final O reference) {
        return tryResolve(scopeGraph, resolutionResult, comparator, reference).map(Tuple2::_1);
    }
    
    @Deprecated
    private static <S extends IScope, L extends ILabel, O extends IOccurrence, V> Set.Immutable<O> resolveToDeclarations(final IEsopScopeGraph<S, L, O, V> scopeGraph, final AllShortestPathsResult<S, L, O> resolutionResult, final Comparator<Distance<L>> comparator, final O reference) {
        return tryResolve(scopeGraph, resolutionResult, comparator, reference).map(Tuple2::_1).orElse(Set.Immutable.of()).stream().map(IResolutionPath::getDeclaration).collect(CapsuleCollectors.toSet());
    }    
    
    private static <S extends IScope, L extends ILabel, O extends IOccurrence, V> Optional<Tuple2<Set.Immutable<IResolutionPath<S, L, O>>, Set.Immutable<String>>> tryResolve(
            final IEsopScopeGraph<S, L, O, V> scopeGraph, final AllShortestPathsResult<S, L, O> resolutionResult,
            final Comparator<Distance<L>> comparator, final O reference) {

        if (resolutionResult.isFinal) {
//            throw new UnsupportedOperationException("Not yet implemented.");
           
            final Set.Immutable<IResolutionPath<S, L, O>> paths = resolutionResult.parameters.resolvedImportPaths().get(reference);
            
            if (!paths.isEmpty()) {
                final Set.Immutable<String> messages = Set.Immutable.of();  
                return Optional.of(ImmutableTuple2.of(paths, messages));
            }
            
//            final Set.Immutable<ScopeLabelOccurrence<S, L, O>> sloReferences = resolutionResult.parameters.resolvedImports.keySet().stream()
//                    .filter(slo -> slo.occurrence().equals(reference))
//                    .collect(CapsuleCollectors.toSet()); 
//                       
//            if (!sloReferences.isEmpty()) {            
//                // final Set.Immutable<IResolutionPath<S, L, O>> paths = resolutionResult.resolvedImports.get(sloReferences.get());
//    
//                // TOOD: check resolutionResult.resolvedImports need to be a multi-map?!
//                final Set.Immutable<IResolutionPath<S, L, O>> paths = sloReferences.stream().flatMap(sloReference -> resolutionResult.parameters.resolvedImports.get(sloReference).stream()).collect(CapsuleCollectors.toSet());       
//                final Set.Immutable<String> messages = Set.Immutable.of(); // TODO save and cache messages while calculating shortest paths  
//                return Optional.of(ImmutableTuple2.of(paths, messages));
//            }
        }        
        
        final int u = resolutionResult.reverseIndex.get(reference);
        final Distance<L>[] visibleTargets = resolutionResult.dist[u];
                
        final Set.Immutable<Integer> candidateIndices = scopeGraph.declarationEdgeStream()
                .filter(slo -> IOccurrence.match(reference, slo.occurrence()))
                .map(tuple -> tuple.occurrence())
                .mapToInt(resolutionResult.reverseIndex::get)
                .boxed()
                .collect(CapsuleCollectors.toSet());

        if (candidateIndices.isEmpty()) {
            return Optional.of(ImmutableTuple2.of(Set.Immutable.of(),
                    Set.Immutable.of("Does not resolve.", "No reachable declarations with matching identifier name.")));
        }
        
        final Set.Immutable<Integer> declarationIndices = cheapest(visibleTargets, candidateIndices, comparator);
        
        final Set.Immutable<Distance<L>> declarationsCosts = declarationIndices.stream()
                .map(index -> visibleTargets[index])
                .collect(CapsuleCollectors.toSet());
        
        final Set.Immutable<O> declarations = declarationIndices.stream()
                .map(index -> (O) resolutionResult.forwardIndex.get(index))
                .collect(CapsuleCollectors.toSet());

        if (declarationsCosts.contains(Distance.INFINITE)) {            
            /*
             * NOTE: type-based name resolution depends on the fact that do-not-know (i.e., Optional.empty()) is returned here, 
             * instead of signaling that no resolution was found (i.e., Set.Immutable.of()).
             */            
            // TODO: maybe check if edge is closed. If yes, then return empty Set.Immutable.of(), other Optional.empty()?
            return Optional.empty();
        }
        
        final Set.Immutable<IResolutionPath<S, L, O>> resolutionPaths = 
                declarations.stream()
                    .map(declaration -> resolutionResult.path(reference, declaration).get())
                    .collect(CapsuleCollectors.toSet());
        
        final Set.Immutable<String> messages = Set.Immutable.of();
                // .of(path.getDeclaration().getIndex().getResource());
        
        return Optional.of(ImmutableTuple2.of(resolutionPaths, messages));
    }   

    private final static <L extends ILabel> Set.Immutable<Integer> cheapest(final Distance<L>[] distances, final Set.Immutable<Integer> candidates, final Comparator<Distance<L>> comparator) {
        final OptionalInt declarationIndexAtMinimalCost = IntStream.range(0, distances.length)
                .filter(candidates::contains)
                .reduce((i, j) -> comparator.compare(distances[i], distances[j]) < 0 ? i : j);
        
        /*
         * NOTE: minimalDistance is an example instance of at minimal cost.
         * However, there could be ambiguous instance with the same cost.
         */
        final Distance<L> minimalDistanceExample = distances[declarationIndexAtMinimalCost.getAsInt()];
        
        final Set.Immutable<Integer> indices = IntStream.range(0, distances.length)
                .filter(candidates::contains)
                .filter(index -> comparator.compare(distances[index], minimalDistanceExample) == 0)
                .mapToObj(Integer::valueOf).collect(CapsuleCollectors.toSet());
        
        return indices;
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
    public IEsopNameResolution.Transient<S, L, O> melt(IEsopScopeGraph<S, L, O, ?> scopeGraph, Predicate2<S, L> isEdgeClosed) {
        return new AllShortestPathsNameResolutionBuilder<>(this, isEdgeClosed);
    }

}
