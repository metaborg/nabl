package org.metaborg.meta.nabl2.spoofax.analysis;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class EditorMessage {

    @Value.Parameter public abstract ITerm getOrigin();

    @Value.Parameter public abstract String getMessage();

    public static IMatcher<EditorMessage> matcher() {
        return M.tuple2(M.term(), M.term(), (t, o, msg) -> {
            return ImmutableEditorMessage.of(o, msg.toString());
        });
    }

}