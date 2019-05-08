package mb.nabl2.solver.components;

import java.util.Optional;

import mb.nabl2.constraints.equality.ImmutableCEqual;
import mb.nabl2.constraints.relations.CBuildRelation;
import mb.nabl2.constraints.relations.CCheckRelation;
import mb.nabl2.constraints.relations.CEvalFunction;
import mb.nabl2.constraints.relations.IRelationConstraint;
import mb.nabl2.relations.IFunctionName;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.ISolver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;


public class ExternalRelationComponent extends ASolver {

    public ExternalRelationComponent(SolverCore core) {
        super(core);
    }

    public Optional<SolveResult> solve(IRelationConstraint constraint) {
        return constraint.match(IRelationConstraint.Cases.of(this::solve, this::solve, this::solve));
    }

    // ------------------------------------------------------------------------------------------------------//

    public Optional<SolveResult> solve(CBuildRelation c) {
        return Optional.empty();
    }

    public Optional<SolveResult> solve(CCheckRelation c) {
        return Optional.empty();
    }

    public Optional<SolveResult> solve(CEvalFunction c) {
        if(!unifier().isGround(c.getTerm())) {
            return Optional.empty();
        }
        final ITerm term = unifier().findRecursive(c.getTerm());
        return c.getFunction().match(IFunctionName.Cases.of(
        // @formatter:off
            name -> {
                return Optional.empty();
            },
            extName -> {
                return callExternal(extName, term).map(ret -> {
                    return SolveResult.constraints(ImmutableCEqual.of(c.getResult(), ret, c.getMessageInfo()));
                });
            }
            // @formatter:on
        ));
    }

}