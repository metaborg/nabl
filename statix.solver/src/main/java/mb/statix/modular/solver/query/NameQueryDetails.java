package mb.statix.modular.solver.query;

import java.util.List;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.modular.name.NameAndRelation;
import mb.statix.modular.scopegraph.reference.TrackingNameResolution;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.terms.Scope;

public class NameQueryDetails extends QueryDetails {
    private static final long serialVersionUID = 1L;
    
    private final NameAndRelation name;
    public NameQueryDetails(String owner, NameAndRelation name, CResolveQuery constraint, Multimap<Scope, LabelWF<ITerm>> edges,
            Multimap<Scope, LabelWF<ITerm>> data, List<ITerm> queryResult) {
        super(owner, constraint, edges, data, queryResult);
        this.name = name;
    }
    
    public NameQueryDetails(String owner, NameAndRelation name, CResolveQuery constraint,
            TrackingNameResolution<Scope, ITerm, ITerm> nameResolution, List<ITerm> queryResult) {
        super(owner, constraint, nameResolution, queryResult);
        this.name = name;
    }

    public NameAndRelation getName() {
        return this.name;
    }

}
