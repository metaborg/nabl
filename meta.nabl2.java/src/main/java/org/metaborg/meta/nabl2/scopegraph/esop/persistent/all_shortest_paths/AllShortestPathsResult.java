package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.Set;

public class AllShortestPathsResult<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Serializable {
    
    private static final long serialVersionUID = 42L;
    
    public final Distance<L>[][] dist;
    public final int[][] next;

    private final java.util.Map<Object, Integer> graphNodeToIdentifier; // reverse index
    private final java.util.Map<Integer, Object> identifierToGraphNode; // forward index

    public final AllShortestPathsParameters<S, L, O> parameters;
    
    private boolean isFinal = false;

    public final int graphNodeToIdentifier(final Object graphNode) {
        final Integer identifier = graphNodeToIdentifier.get(graphNode);
        
        if (identifier == null) {
            throw new IllegalArgumentException(String.format("No index found for graph node %s.", graphNode));
        } else {
            return identifier.intValue();
        }
    }
    
    public final <T> T identifierToGraphNode(final int identifier) {
        final Object graphNode = identifierToGraphNode.get(identifier);
        
        if (graphNode == null) {
            throw new IllegalArgumentException(String.format("No graph node found for identifier %s.", identifier));
        } else {
            return (T) graphNode;
        }
    }    
    
    public final int occurrenceToIdentifier(final O occurrence) {
        return graphNodeToIdentifier(occurrence);
    }
    
    public final O identifierToOccurrence(final int identifier) {
        return identifierToGraphNode(identifier);
    }    

    public final int scopeToIdentifier(final S scope) {
        return graphNodeToIdentifier(scope);
    }
    
    public final S identifierToScope(final int identifier) {
        return identifierToGraphNode(identifier);
    }    
        
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal() {
        isFinal = true;
    }
    
    @SuppressWarnings("unchecked")
    private AllShortestPathsResult() {
        this.dist = new Distance[0][0];
        this.next = new int[0][0];

        this.graphNodeToIdentifier = Collections.EMPTY_MAP;
        this.identifierToGraphNode = Collections.EMPTY_MAP;

        this.parameters = new AllShortestPathsParameters<>(Set.Immutable.of());
    }
    
    public static final <S extends IScope, L extends ILabel, O extends IOccurrence> AllShortestPathsResult<S, L, O> empty() {
        return new AllShortestPathsResult<>();
    }
    
    public AllShortestPathsResult(final Distance<L>[][] dist, final int[][] next,
            final java.util.Map<Object, Integer> graphNodeToIdentifier, final java.util.Map<Integer, Object> identifierToGraphNode,
            final AllShortestPathsParameters<S, L, O> parameters) {
        this.dist = dist;
        this.next = next;

        this.graphNodeToIdentifier = graphNodeToIdentifier;
        this.identifierToGraphNode = identifierToGraphNode;

        this.parameters = parameters;
    }
    
    /*
     * Reconstruct path from all-shortest-paths matrix.
     */
    public Optional<IResolutionPath<S, L, O>> path(final O reference, final O declaration) {
        
        if (!IOccurrence.match(reference, declaration)) {
            throw new IllegalArgumentException(String.format("Reference and declaration must match.\n   ref: %s\n   dec: %s", reference, declaration));
        }
        
        final int u = graphNodeToIdentifier.get(reference);

        int j = u;
        final int k = graphNodeToIdentifier.get(declaration);

        /*
         * if next[u][v] = null then 
         *   return [] 
         *   
         * path = [u] 
         * while u ≠ v 
         *   u ← next[u][v] 
         *   path.append(u) 
         *   
         * return path
         */
        final List<Object> trace = new ArrayList<>(0);

        if (next[j][k] == -1) {
            return Optional.empty();
        } else {
            trace.add(identifierToGraphNode.get(j));

            while (j != k) {
                j = next[j][k];                
                trace.add(identifierToGraphNode.get(j));
            }
        }

        return traceToPath(trace, dist[u][k].getLabels());
    }
    
    /**
     * Converts the a trace of occurrences/scopes (that are separated by labeled steps) into a resolution path.
     * 
     * @param trace the sequence of occurrences and scopes that form a path
     * @param labels the sequence of labeled steps separating the trace
     * @param substitutionEvidence lookup table for named import edges
     * @return a resolution path from a reference to its declaration 
     */
    @SuppressWarnings("unchecked")
    private Optional<IResolutionPath<S, L, O>> traceToPath(final List<?> trace, final List<L> labels) {
        assert labels.size() == trace.size() - 1;
        
        final O reference = (O) trace.get(0);        
        final List<S> scopes = trace.stream().skip(1).limit(trace.size() - 2).map(occurrence -> (S) occurrence).collect(Collectors.toList());      
        final O declaration = (O) trace.get(trace.size() - 1);

        IScopePath<S, L, O> pathSegment = Paths.empty(scopes.get(0));        
        S pathTarget = scopes.get(0);
        
        for (int i = 1; i < scopes.size(); i++) {
            final L nextLabel = labels.get(i);
            final S nextScope = scopes.get(i);
            
            final ScopeLabelScope<S, L, O> directEdge = ImmutableScopeLabelScope.of(pathTarget, nextLabel, nextScope);

            if (parameters.resolvedImportEdges().contains(directEdge)) {
                pathSegment = Paths.append(pathSegment, Paths.named(pathTarget, nextLabel, parameters.resolvedImportPath(directEdge), nextScope)).get();
                pathTarget  = nextScope;
            } else {
                pathSegment = Paths.append(pathSegment, Paths.direct(pathTarget, nextLabel, nextScope)).get();
                pathTarget  = nextScope;
            }
        }
        
        return Paths.resolve(reference, pathSegment, declaration);
    }    
    
    final void printMatrix() {
        printMatrix(i -> true, i -> true);
    }

    final void printMatrix(IntPredicate rowFilter, IntPredicate colFilter) {
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
    
}
