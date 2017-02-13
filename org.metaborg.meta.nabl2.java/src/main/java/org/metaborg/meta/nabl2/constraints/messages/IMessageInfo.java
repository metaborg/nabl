package org.metaborg.meta.nabl2.constraints.messages;

import org.metaborg.meta.nabl2.terms.ITerm;

public interface IMessageInfo {

    MessageKind getKind();

    IMessageContent getContent();

    ITerm getOriginTerm();

    IMessageInfo withDefault(IMessageContent defaultContent);

}