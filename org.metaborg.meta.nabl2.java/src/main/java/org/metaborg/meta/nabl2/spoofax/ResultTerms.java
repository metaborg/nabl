package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.ConstraintTerms;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.iterators.Iterables2;

public class ResultTerms {

    public static IMatcher<ImmutableInitialResult> initialOf() {
        return M.appl3("InitialResult", ConstraintTerms.constraints(), args(), SolverConfig.matcher(), (t, constraints,
                args, config) -> {
            return ImmutableInitialResult.of(constraints, args, config);
        });
    }

    public static IMatcher<ImmutableUnitResult> unitOf() {
        return M.appl2("UnitResult", M.term(), ConstraintTerms.constraints(), (t, ast, constraints) -> {
            return ImmutableUnitResult.of(ast, constraints);
        });
    }

    public static IMatcher<ImmutableFinalResult> finalOf() {
        return M.appl0("FinalResult", (t) -> {
            return ImmutableFinalResult.of();
        });
    }

    private static IMatcher<Args> args() {
        return M.cases(
            // @formatter:off
            M.appl1("Params", params(), (a,ps) -> ImmutableArgs.of(ps, Optional.empty())),
            M.appl2("ParamsAndType", params(), M.term(), (a, ps, t) -> ImmutableArgs.of(ps, Optional.of(t)))
            // @formatter:on
        );
    }

    private static IMatcher<Iterable<ITerm>> params() {
        return M.cases(
            // @formatter:off
            M.appl("", (a) -> a.getArgs()),
            M.term(p -> Iterables2.singleton(p))
            // @formatter:on
        );
    }

}