package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class FinalResult {

    @Value.Auxiliary public abstract Optional<ITerm> getCustomResult();

    public abstract FinalResult withCustomResult(Optional<? extends ITerm> customResult);

    public static IMatcher<FinalResult> matcher() {
        return M.appl0("FinalResult", (t) -> {
            return ImmutableFinalResult.of();
        });
    }

}