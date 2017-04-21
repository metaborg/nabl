package org.metaborg.meta.nabl2.spoofax.analysis;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageKind;
import org.metaborg.meta.nabl2.solver.messages.IMessages;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

@Value.Immutable
@Serial.Version(value = 42L)
public abstract class CustomSolution {

    @Value.Parameter public abstract ITerm getAnalysis();

    @Value.Parameter public abstract IMessages getMessages();

    public static IMatcher<CustomSolution> matcher() {
        return M.tuple4(M.listElems(MessageInfo.matcherEditorMessage(MessageKind.ERROR)),
                M.listElems(MessageInfo.matcherEditorMessage(MessageKind.WARNING)),
                M.listElems(MessageInfo.matcherEditorMessage(MessageKind.NOTE)), M.term(), (t, es, ws, ns, a) -> {
                    Messages messages = new Messages();
                    messages.addAll(es);
                    messages.addAll(ws);
                    messages.addAll(ns);
                    return ImmutableCustomSolution.of(a, messages);
                });
    }

}