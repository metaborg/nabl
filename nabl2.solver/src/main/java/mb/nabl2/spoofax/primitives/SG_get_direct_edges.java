package mb.nabl2.spoofax.primitives;

import org.metaborg.util.collection.IRelation3;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.pepm16.IScopeGraph;
import mb.scopegraph.pepm16.terms.Label;
import mb.scopegraph.pepm16.terms.Occurrence;
import mb.scopegraph.pepm16.terms.Scope;

public class SG_get_direct_edges extends ScopeGraphEdgePrimitive<Scope> {

    public SG_get_direct_edges() {
        super(SG_get_direct_edges.class.getSimpleName());
    }

    @Override protected IMatcher<Scope> getSourceMatcher() {
        return Scope.matcher();
    }

    @Override protected IRelation3<Scope, Label, ? extends ITerm>
            getEdges(IScopeGraph<Scope, Label, Occurrence> scopeGraph) {
        return scopeGraph.getDirectEdges();
    }

}