package mb.statix.solver.query;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.MatchException;
import mb.nabl2.util.Tuple2;
import mb.statix.scopegraph.reference.DataEquiv;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.IDebugContext;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
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

    public boolean eq(List<ITerm> datum1, List<ITerm> datum2) throws ResolutionException, InterruptedException {
        try {
            final Tuple2<State, Lambda> result =
                    constraint.apply(ImmutableList.of(B.newTuple(datum1), B.newTuple(datum2)), state);
            final Config config = Config.of(result._1(), result._2().getBody(), completeness);
            return Solver.entails(config, result._2().getBodyVars(), debug)
                    .orElseThrow(() -> new ResolutionException("Data equivalence check delayed"));
        } catch(MatchException ex) {
            return false;
        }
    }

    public boolean alwaysTrue() {
        return false;
    }

}