package org.metaborg.meta.nabl2.scopegraph.esop.persistent.all_shortest_paths;

import java.io.Serializable;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelOccurrence;
import org.metaborg.meta.nabl2.util.tuples.ScopeLabelScope;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;

public class AllShortestPathsParameters<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements Serializable {

    private static final long serialVersionUID = 42L;
           
    private final Set.Immutable<O> unresolvedImports;
    private final SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> resolvedImports;
    private final SetMultimap.Immutable<O, ScopeLabelScope<S, L, O>> invalidImports;
                
    private final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> directEdgeToResolutionPath;
    private final Map.Immutable<ScopeLabelScope<S, L, O>, IResolutionPath<S, L, O>> invalidDirectEdgeToResolutionPath;
    
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
    
    public static final boolean isFixpointReached(AllShortestPathsParameters<?, ?, ?> one, AllShortestPathsParameters<?, ?, ?> two) {
        return one.resolvedImports.size() == two.resolvedImports.size() && one.invalidImports.size() == two.invalidImports.size();
    }    

    /**********************************************************************
     * REFERENCES
     **********************************************************************/      
    
    public Set.Immutable<O> resolvedImportReferences() {
        return resolvedImports.keySet().stream().collect(CapsuleCollectors.toSet());
    }
    
    public Set.Immutable<O> unresolvedImportReferences() {
        return unresolvedImports;
    }
    
    /**********************************************************************
     * EDGES
     **********************************************************************/

    public Set.Immutable<ScopeLabelScope<S, L, O>> resolvedImportEdges() {
        return directEdgeToResolutionPath.keySet().stream().collect(CapsuleCollectors.toSet());
    }
    
    public Set.Immutable<ScopeLabelScope<S, L, O>> resolvedImportEdges(final O importReference) {
        return resolvedImports.get(importReference);
    }    
    
    /**********************************************************************
     * PATHS
     **********************************************************************/
    
    public Set.Immutable<IResolutionPath<S, L, O>> resolvedImportPaths() {
        return directEdgeToResolutionPath.values().stream().collect(CapsuleCollectors.toSet());
    }

    public Set.Immutable<IResolutionPath<S, L, O>> resolvedImportPaths(final O importReference) {        
        return resolvedImports.get(importReference).stream()
                .map(directEdgeToResolutionPath::get)
                .collect(CapsuleCollectors.toSet());
    }
    
    public IResolutionPath<S, L, O> resolvedImportPath(ScopeLabelScope<S, L, O> directEdge) {
        return directEdgeToResolutionPath.get(directEdge);
    }    
    
    /**********************************************************************
     * ...
     **********************************************************************/
    
    public boolean isImportEdgeInvalidated(O importReference, ScopeLabelScope<S, L, O> directEdge) {
        return this.invalidImports.containsEntry(importReference, directEdge);
    }
            
    public AllShortestPathsParametersBuilder<S, L, O> asTransient() {
        return new AllShortestPathsParametersBuilder<>(
                unresolvedImports.asTransient(), 
                resolvedImports.asTransient(), 
                invalidImports.asTransient(), 
                directEdgeToResolutionPath.asTransient(), 
                invalidDirectEdgeToResolutionPath.asTransient());
    }

    @Override
    public String toString() {
        return String.format(
                "ShortestPathParameters [\nisFinal=%s, \nunresolvedImports=%s, \nresolvedImports=%s, \ndirectEdgeToResolutionPath=%s, \ninvalidDirectEdgeToResolutionPath=%s]",
                isFinal, unresolvedImports, resolvedImports, directEdgeToResolutionPath, invalidDirectEdgeToResolutionPath);
    }

}