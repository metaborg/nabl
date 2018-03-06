package mb.nabl2.spoofax.primitives;

import mb.nabl2.scopegraph.IScopeGraph;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.collections.IRelation3;

public class SG_get_direct_edges_inv extends ScopeGraphEdgePrimitive<Scope> {

    public SG_get_direct_edges_inv() {
        super(SG_get_direct_edges_inv.class.getSimpleName());
    }

    @Override protected IMatcher<Scope> getSourceMatcher() {
        return Scope.matcher();
    }

    @Override protected IRelation3<Scope, Label, ? extends ITerm>
            getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return scopeGraph.getDirectEdges().inverse();
    }

}