package mb.statix.taico.solver.query;

import java.util.List;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CResolveQuery;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.taico.name.Name;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;

public class NameQueryDetails extends QueryDetails {
    private static final long serialVersionUID = 1L;
    
    private final Name name;
    public NameQueryDetails(String owner, Name name, CResolveQuery constraint, Multimap<Scope, LabelWF<ITerm>> edges,
            Multimap<Scope, LabelWF<ITerm>> data, List<ITerm> queryResult) {
        super(owner, constraint, edges, data, queryResult);
        this.name = name;
    }
    
    public NameQueryDetails(String owner, Name name, CResolveQuery constraint,
            TrackingNameResolution<Scope, ITerm, ITerm> nameResolution, List<ITerm> queryResult) {
        super(owner, constraint, nameResolution, queryResult);
        this.name = name;
    }

    public Name getName() {
        return this.name;
    }

}
