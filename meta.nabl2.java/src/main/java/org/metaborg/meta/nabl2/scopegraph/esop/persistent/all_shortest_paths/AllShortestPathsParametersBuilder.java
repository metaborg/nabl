package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsParametersBuilder<S extends IScope, L extends ILabel, O extends IOccurrence> {

    public Set.Transient<O> unresolvedImports;
    public SetMultimap.Transient<O, ScopeLabelScope<S, L, O>> resolvedImports;
    public SetMultimap.Transient<O, ScopeLabelScope<S, L, O>> invalidImports;

    // NOTE: previous name was (invalidated)SubstitutionEvidence 
    public Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath;
    public Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidDirectEdgeToResolutionPath;
    
    public AllShortestPathsParametersBuilder(
            final Set.Transient<O> unresolvedImports, 
            final SetMultimap.Transient<O, ScopeLabelScope<S, L, O>> resolvedImports,
            final SetMultimap.Transient<O, ScopeLabelScope<S, L, O>> invalidImports,
            final Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath,
            final Map.Transient<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidDirectEdgeToResolutionPath) {
        this.unresolvedImports = unresolvedImports;
        this.resolvedImports = resolvedImports;
        this.invalidImports = invalidImports;
        this.directEdgeToResolutionPath = directEdgeToResolutionPath;
        this.invalidDirectEdgeToResolutionPath = invalidDirectEdgeToResolutionPath;
    }

    // TODO make public API
    // TODO remove duplicate (persistent / transient)
    public SetMultimap.Immutable<O, IResolutionPath<S, L, O>> resolvedImportPaths() {
        // joins resolvedImports with directEdgeToResolutionPath            
        return resolvedImports.entrySet().stream()
                .map(tuple -> ImmutableTuple2.of(tuple.getKey(), directEdgeToResolutionPath.get(tuple.getValue())))
                .collect(CapsuleCollectors.toSetMultimap(tuple -> tuple._1(), tuple -> tuple._2()));
    }        
    
    public void resolveImport(O importReference, final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath) {              
         assert this.unresolvedImports.contains(importReference);
         assert !this.resolvedImports.containsKey(importReference);

        this.unresolvedImports.__remove(importReference);

        for (ScopeLabelScope<S, L, O> directEdge : directEdgeToResolutionPath.keySet()) {            
            this.resolvedImports.__insert(importReference, directEdge);
            this.directEdgeToResolutionPath.__put(directEdge, directEdgeToResolutionPath.get(directEdge));
        }
    }
    
    /**
     * Updating import resolution by:
     * 
     * a) moving invalidated edges and paths from the current solution to the log, and
     * b) inserting new edges and paths. 
     */
    public void updateImport(O importReference, final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath) {

        assert this.resolvedImports.containsKey(importReference);
        assert !directEdgeToResolutionPath.isEmpty();
        
        // remove
        final Set.Immutable<ScopeLabelScope<S, L, O>> oldDirectEdges = this.resolvedImports.get(importReference);
        this.resolvedImports.__remove(importReference);
        
        // move to invalidated
        oldDirectEdges.forEach(directEdge -> this.invalidImports.__insert(importReference, directEdge));

        
        
        // move to invalidated 
        oldDirectEdges.forEach(directEdge -> {
            this.invalidDirectEdgeToResolutionPath.__put(directEdge, this.directEdgeToResolutionPath.get(directEdge));
        });
        
        // remove
        oldDirectEdges.forEach(directEdge -> {                
            this.directEdgeToResolutionPath.__remove(directEdge);
        });
        
        // add new data
        for (ScopeLabelScope<S, L, O> directEdge : directEdgeToResolutionPath.keySet()) {            
            this.resolvedImports.__insert(importReference, directEdge);
            this.directEdgeToResolutionPath.__put(directEdge, directEdgeToResolutionPath.get(directEdge));
        }            
        
    }

    public AllShortestPathsParameters<S, L, O> freeze() {
        return new AllShortestPathsParameters<>(unresolvedImports.freeze(), resolvedImports.freeze(), invalidImports.freeze(), directEdgeToResolutionPath.freeze(), invalidDirectEdgeToResolutionPath.freeze());
    }
}