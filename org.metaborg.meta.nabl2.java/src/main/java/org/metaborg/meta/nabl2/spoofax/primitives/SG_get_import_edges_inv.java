package org.metaborg.meta.nabl2.spoofax.primitives;

import org.metaborg.meta.nabl2.scopegraph.IScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public class SG_get_import_edges_inv extends ScopeGraphEdgePrimitive<Occurrence> {

    public SG_get_import_edges_inv() {
        super(SG_get_import_edges_inv.class.getSimpleName());
    }

    @Override protected IMatcher<Occurrence> getSourceMatcher() {
        return Occurrence.matcher();
    }

    @Override protected IRelation3<Occurrence, Label, ? extends ITerm>
            getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return scopeGraph.getImportEdges().inverse();
    }

}