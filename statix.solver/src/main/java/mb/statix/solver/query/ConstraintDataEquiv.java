package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.MatchException;
import mb.nabl2.terms.unification.CannotUnifyException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Delay;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Lambda;

public class ConstraintDataEquiv implements DataEquiv<ITerm> {

    private final Lambda constraint;
    private final State state;
    private final Completeness completeness;
    private final IDebugContext debug;

    public ConstraintDataEquiv(Lambda constraint, State state, Completeness completeness, IDebugContext debug) {
        this.constraint = constraint;
        this.state = state;
        this.completeness = completeness;
        this.debug = debug;
    }

    @Override public boolean eq(List<ITerm> datum1, List<ITerm> datum2)
            throws ResolutionException, InterruptedException {
        try {
            final ITerm term1 = B.newTuple(datum1);
            final ITerm term2 = B.newTuple(datum2);
            final Tuple2<State, Lambda> result = constraint.apply(ImmutableList.of(term1, term2), state);
            try {
                if(Solver.entails(result._1(), result._2().getBody(), completeness, result._2().getBodyVars(), debug)
                        .isPresent()) {
                    debug.info("{} shadows {}", state.unifier().toString(term1), state.unifier().toString(term2));
                    return true;
                } else {
                    debug.info("{} does not shadow {}", state.unifier().toString(term1),
                            state.unifier().toString(term2));
                    return false;
                }
            } catch(Delay d) {
                throw new ResolutionDelayException("Data order delayed.", d);
            }
        } catch(MatchException | CannotUnifyException ex) {
            return false;
        }
    }

    @Override public boolean alwaysTrue() throws InterruptedException {
        return constraint.isAlways(state.spec()).orElse(false);
    }

}