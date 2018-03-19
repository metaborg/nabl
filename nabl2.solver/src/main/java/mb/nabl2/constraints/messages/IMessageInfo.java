package mb.nabl2.constraints.messages;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;

public interface IMessageInfo {

    MessageKind getKind();

    IMessageContent getContent();

    ITerm getOriginTerm();

    IMessageInfo withDefaultContent(IMessageContent defaultContent);

    IMessageInfo withContent(IMessageContent content);

    IMessageInfo apply(Function1<ITerm, ITerm> f);

}