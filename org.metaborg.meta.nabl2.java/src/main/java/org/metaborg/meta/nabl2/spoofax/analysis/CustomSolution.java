package org.metaborg.meta.nabl2.spoofax.analysis;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.solver.Message;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CustomSolution {

    @Value.Parameter public abstract List<Message> getErrors();

    @Value.Parameter public abstract List<Message> getWarnings();

    @Value.Parameter public abstract List<Message> getNotes();

    @Value.Parameter public abstract ITerm getAnalysis();

    public static IMatcher<CustomSolution> matcher() {
        return M.tuple4(M.listElems(Message.matcher()), M.listElems(Message.matcher()), M.listElems(Message.matcher()),
                M.term(), (t, es, ws, ns, a) -> {
                    return ImmutableCustomSolution.of(es, ws, ns, a);
                });
    }

}