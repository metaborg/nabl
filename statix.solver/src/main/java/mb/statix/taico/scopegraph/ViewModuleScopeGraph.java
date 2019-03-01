package mb.statix.taico.scopegraph;

import io.usethesource.capsule.Set.Immutable;
import mb.nabl2.terms.ITerm;

public class ViewModuleScopeGraph extends ModuleScopeGraph {
    private ModuleScopeGraph msg;
    public ViewModuleScopeGraph(ModuleScopeGraph msg) {
        super(msg.owner, msg.labels, msg.endOfPath, msg.relations, msg.canExtend);
    }
    
    @Override
    public Immutable<ITerm> getLabels() {
        return msg.getLabels();
    }
}
