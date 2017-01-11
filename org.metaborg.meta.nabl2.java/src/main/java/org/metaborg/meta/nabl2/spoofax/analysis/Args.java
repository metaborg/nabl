package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.iterators.Iterables2;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Args {

    @Value.Parameter public abstract Iterable<ITerm> getParams();

    @Value.Parameter public abstract Optional<ITerm> getType();

    public static IMatcher<Args> matcher() {
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