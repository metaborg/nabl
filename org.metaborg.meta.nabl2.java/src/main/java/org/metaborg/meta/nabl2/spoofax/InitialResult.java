package org.metaborg.meta.nabl2.spoofax;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.ConstraintTerms;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class InitialResult {

    @Value.Parameter public abstract Iterable<IConstraint> getConstraints();

    @Value.Parameter public abstract Args getArgs();

    @Value.Parameter public abstract SolverConfig getConfig();

    public abstract Optional<ITerm> getCustomResult();

    public static IMatcher<ImmutableInitialResult> matcher() {
        return M.appl3("InitialResult", ConstraintTerms.constraints(), Args.matcher(), SolverConfig.matcher(), (t, constraints,
                args, config) -> {
            return ImmutableInitialResult.of(constraints, args, config);
        });
    }

}