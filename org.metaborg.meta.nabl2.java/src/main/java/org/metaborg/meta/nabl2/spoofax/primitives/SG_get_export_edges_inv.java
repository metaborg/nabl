package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public class SG_get_export_edges_inv extends ScopeGraphEdgePrimitive<Scope> {

    public SG_get_export_edges_inv() {
        super(SG_get_export_edges_inv.class.getSimpleName());
    }

    @Override protected IMatcher<Scope> getSourceMatcher() {
        return Scope.matcher();
    }
    
    @Override protected IRelation3<Scope, Label, ? extends ITerm>
            getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return scopeGraph.getExportEdges().inverse();
    }
    
}