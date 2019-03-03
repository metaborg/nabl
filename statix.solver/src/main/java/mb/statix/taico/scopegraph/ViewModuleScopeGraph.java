package mb.statix.taico.scopegraph;

import java.util.Collection;

import mb.nabl2.terms.ITerm;

/**
 * Class which provides a view of a scope graph at a particular state.
 * 
 * A ViewModuleScopeGraph can be modified without changes to the original module scope graph.
 * 
 * Changes to the original module scope graph after the creation of the view are NOT reflected. 
 */
public class ViewModuleScopeGraph extends ModuleScopeGraph {
    //TODO Get rid of these horrible generics issues
    @SuppressWarnings("unchecked")
    public ViewModuleScopeGraph(IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> msg) {
        super(null,
                msg.getOwner(),
                msg.getLabels(),
                msg.getEndOfPath(),
                msg.getRelations(),
                (Iterable<? extends IOwnableScope>) msg.getExtensibleScopes());
        this.scopes.addAll((Collection<? extends IOwnableScope>) msg.getScopes());
        this.edges.putAll(msg.getEdges());
        this.data.putAll(msg.getData());
        
        for (IMInternalScopeGraph<IOwnableTerm, ITerm, ITerm, ITerm> child : msg.getChildren()) {
            this.children.add(new ViewModuleScopeGraph(child));
        }
    }
}