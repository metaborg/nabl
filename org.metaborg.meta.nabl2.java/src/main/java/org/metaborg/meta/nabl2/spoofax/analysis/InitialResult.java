package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.Optional;
import java.util.Set;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.Constraints;
import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class InitialResult {

    @Value.Parameter public abstract Set<IConstraint> getConstraints();

    @Value.Parameter public abstract Args getArgs();

    @Value.Parameter public abstract SolverConfig getConfig();

    @Value.Auxiliary public abstract Optional<ITerm> getCustomResult();

    public abstract InitialResult withCustomResult(Optional<? extends ITerm> customResult);

    public static IMatcher<InitialResult> matcher() {
        return M.appl3("InitialResult", M.listElems(Constraints.matcher()), Args.matcher(), SolverConfig.matcher(),
                (t, constraints, args, config) -> {
                    return ImmutableInitialResult.of(constraints, args, config);
                });
    }

}