package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.io.Serializable;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsParameters<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Serializable {

    private static final long serialVersionUID = 42L;
           
    public Set.Immutable<O> unresolvedImports;
    public SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> resolvedImports;
    public SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> invalidImports;
    
    // TODO: join to product public SetMultimap.Immutable<O, IResolutionPath<S, L, O>> resolvedImportPaths;
            
    public Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath;
    public Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidDirectEdgeToResolutionPath;
    
    public boolean isFinal = false;
    
    public AllShortestPathsParameters(final Set.Immutable<ScopeLabelOccurrence<S, L, O>> unresolvedImportEdges) {
        this.unresolvedImports = unresolvedImportEdges.stream().map(ScopeLabelOccurrence::occurrence).collect(CapsuleCollectors.toSet());
        this.resolvedImports = SetMultimap.Immutable.of();
        this.invalidImports = SetMultimap.Immutable.of();
        this.directEdgeToResolutionPath = Map.Immutable.of();
        this.invalidDirectEdgeToResolutionPath = Map.Immutable.of();
    }
    
    public AllShortestPathsParameters(
            final Set.Immutable<O> unresolvedImports,
            final SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> resolvedImports,
            final SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> invalidImports,
            final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath,
            final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidDirectEdgeToResolutionPath) {
        this.unresolvedImports = unresolvedImports;
        this.resolvedImports = resolvedImports;
        this.invalidImports = invalidImports;
        this.directEdgeToResolutionPath = directEdgeToResolutionPath;
        this.invalidDirectEdgeToResolutionPath = invalidDirectEdgeToResolutionPath;            
    }
    
    
    public boolean isImportEdgeInvalidated(O importReference, ScopeLabelScope<S, L, O> directEdge) {
        return this.invalidImports.containsEntry(importReference, directEdge);
    }
    
    // TODO make public API
    // TODO remove duplicate (persistent / transient)
    public SetMultimap.Immutable<O, IResolutionPath<S, L, O>> resolvedImportPaths() {
        // joins resolvedImports with directEdgeToResolutionPath            
        return resolvedImports.entrySet().stream()
                .map(tuple -> ImmutableTuple2.of(tuple.getKey(), directEdgeToResolutionPath.get(tuple.getValue())))
                .collect(CapsuleCollectors.toSetMultimap(tuple -> tuple._1(), tuple -> tuple._2()));
    }        
            
    public AllShortestPathsParametersBuilder<S, L, O> asTransient() {
        return new AllShortestPathsParametersBuilder<>(unresolvedImports.asTransient(), resolvedImports.asTransient(), invalidImports.asTransient(), directEdgeToResolutionPath.asTransient(), invalidDirectEdgeToResolutionPath.asTransient());
    }

    @Override
    public String toString() {
        return String.format(
                "ShortestPathParameters [\nisFinal=%s, \nunresolvedImports=%s, \nresolvedImports=%s, \ndirectEdgeToResolutionPath=%s, \ninvalidDirectEdgeToResolutionPath=%s]",
                isFinal, unresolvedImports, resolvedImports, directEdgeToResolutionPath, invalidDirectEdgeToResolutionPath);
    }
}