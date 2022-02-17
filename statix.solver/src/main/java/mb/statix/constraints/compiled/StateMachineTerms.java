package mb.statix.constraints.compiled;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.spoofax.StatixTerms;

public final class StateMachineTerms {

    private static final String STATE_OP = "State";

    private static final String STEP_OP = "Step";

    private static final String RESOLVE_OP = "Resolve";
    private static final String SUBENV_OP = "SubEnv";
    private static final String MERGE_OP = "Merge";
    private static final String SHADOW_OP = "Shadow";
    private static final String CEXP_OP = "CExp";

    private static final String RVAR_OP = "RVar";

    private StateMachineTerms() {
    }

    public static IMatcher<Map<String, State>> states() {
        return M.listElems(state()).map(states -> {
            final ImmutableMap.Builder<String, State> mapBuilder = ImmutableMap.builder();
            for(Tuple2<String, State> state: states) {
                mapBuilder.put(state._1(), state._2());
            }
            return mapBuilder.build();
        });
    }

    public static IMatcher<Tuple2<String, State>> state() {
        return M.appl3(STATE_OP, M.stringValue(), M.listElems(step()), rvar(),
                (appl, name, steps, var) -> Tuple2.of(name, new State(steps, var)));
    }

    public static IMatcher<RStep> step() {
        return M.appl2(STEP_OP, rvar(), rexp(), (appl, var, exp) -> new RStep(var, exp));
    }

    // public static IMatcher<StateMachine> stateM

    public static IMatcher<RExp> rexp() {
        // @formatter:off
        return M.<RExp>casesFix(m -> Iterables2.from(
            M.appl0(RESOLVE_OP, appl -> RResolve.of()),
            M.appl2(SUBENV_OP, StatixTerms.label(), M.stringValue(), (appl, lbl, state) -> new RSubEnv(lbl, state)),
            M.appl1(MERGE_OP, M.listElems(rvar()), (appl, envs) -> new RMerge(envs)),
            M.appl2(SHADOW_OP, rvar(), rvar(), (appl, left, right) -> new RShadow(left, right)),
            M.appl2(CEXP_OP, rvar(), m, (appl, env, exp) -> new RCExp(env, exp))
        ));
        // @formatter:on
    }

    public static IMatcher<RVar> rvar() {
        return M.appl1(RVAR_OP, M.stringValue(), (appl, name) -> new RVar(name));
    }

}
