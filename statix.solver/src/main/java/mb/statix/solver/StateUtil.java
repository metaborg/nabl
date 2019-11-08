package mb.statix.solver;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.collections.IRelation3;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTellRel;
import mb.statix.scopegraph.IScopeGraph;

public class StateUtil {

    public static List<IConstraint> asConstraint(IScopeGraph<? extends ITerm, ? extends ITerm, ? extends ITerm> scopeGraph) {
        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        scopeGraph.getData().forEach((scopeLabel, data) -> {
            data.forEach(datum -> {
                constraints.add(new CTellRel(scopeLabel.getKey(), scopeLabel.getValue(), datum));
            });
        });
        scopeGraph.getEdges().forEach((scopeLabel, scopes) -> {
            scopes.forEach(scope -> {
                constraints.add(new CTellEdge(scopeLabel.getKey(), scopeLabel.getValue(), scope));
            });
        });
        return constraints.build();
    }
    
    public static List<IConstraint> asConstraint(IRelation3<? extends ITerm, ? extends ITerm, ? extends ITerm> termProperties) {
        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        termProperties.stream().forEach(idxPropTerm -> {
            idxPropTerm.apply((idx, prop, term) -> {
                constraints.add(new CAstProperty(idx, prop, term));
                return null;
            });
        });
        return constraints.build();
    }
    
    public static List<IConstraint> asConstraint(IUnifier unifier) {
        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        unifier.equalityMap().forEach((left, right) -> {
            constraints.add(new CEqual(left, right));
        });
        unifier.disequalities().forEach(diseq -> {
            diseq.toTuple().apply((us, left, right) -> {
                constraints.add(new CInequal(us, left, right));
                return null;
            });
        });
        return constraints.build();
    }
    
}