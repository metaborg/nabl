package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.List;
import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;

import com.google.common.collect.ImmutableList;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class Args {

    private static final String PARAMS_AND_TYPE = "ParamsAndType";
    private static final String PARAMS = "Params";

    @Value.Parameter public abstract List<ITerm> getParams();

    @Value.Parameter public abstract Optional<ITerm> getType();

    public static IMatcher<Args> matcher() {
        return M.cases(
            // @formatter:off
            M.appl1(PARAMS, params(), (a,ps) -> ImmutableArgs.of(ps, Optional.empty())),
            M.appl2(PARAMS_AND_TYPE, params(), M.term(), (a, ps, t) -> ImmutableArgs.of(ps, Optional.of(t)))
            // @formatter:on
        );
    }

    public static ITerm build(Args args) {
        List<ITerm> paramTerms = args.getParams();
        ITerm paramsTerm;
        if(paramTerms.size() == 1) {
            paramsTerm = paramTerms.get(0);
        } else {
            paramsTerm = TB.newTuple(paramTerms);
        }
        return args.getType()
                // @formatter:off
                .map(typeTerm -> TB.newAppl(PARAMS_AND_TYPE, paramsTerm, typeTerm))
                .orElseGet(() -> TB.newAppl(PARAMS, paramsTerm));
                // @formatter:on

    }

    private static IMatcher<List<ITerm>> params() {
        return M.<List<ITerm>>cases(
            // @formatter:off
            M.tuple(a -> a.getArgs()),
            M.term(p -> ImmutableList.of(p))
            // @formatter:on
        );
    }

}