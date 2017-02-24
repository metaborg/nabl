package org.metaborg.meta.nabl2.constraints.messages;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.functions.Function1;

public interface IMessageInfo {

    MessageKind getKind();

    IMessageContent getContent();

    ITerm getOriginTerm();

    IMessageInfo withDefault(IMessageContent defaultContent);

    IMessageInfo apply(Function1<ITerm, ITerm> f);
}