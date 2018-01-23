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
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths.AllShortestPathsNameResolution.ShortestPathParameters;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.tuples.ImmutableScopeLabelScope;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;

public class AllShortestPathsResult<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Serializable {
    
    private static final long serialVersionUID = 42L;
    
    public final Distance<L>[][] dist;
    public final int[][] next;

    public final java.util.Map<Object, Integer> reverseIndex;
    public final java.util.Map<Integer, Object> forwardIndex;

    public final ShortestPathParameters<S, L, O> parameters;
    
    public boolean isFinal = false;
    
    @SuppressWarnings("unchecked")
    private AllShortestPathsResult() {
        this.dist = new Distance[0][0];
        this.next = new int[0][0];

        this.reverseIndex = Collections.EMPTY_MAP;
        this.forwardIndex = Collections.EMPTY_MAP;

        this.parameters = new ShortestPathParameters<>(Set.Immutable.of());
    }
    
    public static final <S extends IScope, L extends ILabel, O extends IOccurrence> AllShortestPathsResult<S, L, O> empty() {
        return new AllShortestPathsResult<>();
    }
    
    public AllShortestPathsResult(final Distance<L>[][] dist, final int[][] next,
            final java.util.Map<Object, Integer> reverseIndex, final java.util.Map<Integer, Object> forwardIndex,
            final ShortestPathParameters<S, L, O> parameters) {
        this.dist = dist;
        this.next = next;

        this.reverseIndex = reverseIndex;
        this.forwardIndex = forwardIndex;

        this.parameters = parameters;
    }
    
    /*
     * Reconstruct path from all-shortest-paths matrix.
     */
    public Optional<IResolutionPath<S, L, O>> path(final O reference, final O declaration) {
        
        if (!IOccurrence.match(reference, declaration)) {
            throw new IllegalArgumentException(String.format("Reference and declaration must match.\n   ref: %s\n   dec: %s", reference, declaration));
        }
        
        final int u = reverseIndex.get(reference);

        int j = u;
        final int k = reverseIndex.get(declaration);

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
            trace.add(forwardIndex.get(j));

            while (j != k) {
                j = next[j][k];                
                trace.add(forwardIndex.get(j));
            }
        }

        return traceToPath(trace, dist[u][k].getLabels(), parameters.directEdgeToResolutionPath);
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
    private static <S extends IScope, L extends ILabel, O extends IOccurrence> Optional<IResolutionPath<S, L, O>> traceToPath(final List<?> trace, final List<L> labels, final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> substitutionEvidence) {
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

            if (substitutionEvidence.containsKey(directEdge)) {
                pathSegment = Paths.append(pathSegment, Paths.named(pathTarget, nextLabel, substitutionEvidence.get(directEdge), nextScope)).get();
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
