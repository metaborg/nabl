package mb.statix.taico.solver.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Multimap;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.DataWF;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.solver.IConstraint;
import mb.statix.taico.scopegraph.reference.TrackingNameResolution;

public class QueryDetails<S extends D, L, D> implements IQueryDetails<S, L, D> {
    private static final long serialVersionUID = 1L;

    private IConstraint constraint;
    private Multimap<S, LabelWF<L>> edges;
    private Multimap<S, LabelWF<L>> data;
    private DataWF<D> dataWf;
    private List<ITerm> queryResult;
    private final Function<S, String> scopeOwner;
    private Set<String> modules;

    public QueryDetails(String owner, IConstraint constraint, TrackingNameResolution<S, L, D> nameResolution,
            List<ITerm> queryResult, Function<S, String> scopeOwner) {
        this(owner, constraint, nameResolution.getTrackedEdges(), nameResolution.getTrackedData(),
                nameResolution.getDataWf(), queryResult, scopeOwner);
    }

    public QueryDetails(String owner, IConstraint constraint, Multimap<S, LabelWF<L>> edges,
            Multimap<S, LabelWF<L>> data, DataWF<D> dataWf, List<ITerm> queryResult, Function<S, String> scopeOwner) {
        this.constraint = constraint;
        this.edges = edges;
        this.data = data;
        this.dataWf = dataWf;
        this.queryResult = queryResult;
        this.scopeOwner = scopeOwner;
        this.modules = computeModules(owner);
    }

    private Set<String> computeModules(String owner) {
        Set<String> modules = new HashSet<>();
        edges.keySet().stream().map(scopeOwner).forEach(modules::add);
        data.keySet().stream().map(scopeOwner).forEach(modules::add);
        modules.remove(owner);
        return modules;
    }

    public IConstraint getConstraint() {
        return constraint;
    }

    @Override
    public Multimap<S, LabelWF<L>> getRelevantEdges() {
        return edges;
    }

    @Override
    public Multimap<S, LabelWF<L>> getRelevantData() {
        return data;
    }

    @Override
    public DataWF<D> getDataWellFormedness() {
        return dataWf;
    }

    @Override
    public Set<String> getReachedModules() {
        return modules;
    }

    @Override
    public List<ITerm> getQueryResult() {
        return queryResult;
    }

    @Override
    public String toString() {
        return "QueryDetails<edges=" + edges + ", data=" + data + ", datawf=" + dataWf + ">";
    }
}
