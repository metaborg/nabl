package mb.nabl2.constraints.messages;

import java.util.function.Function;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;

public interface IMessageContent {

    IMessageContent apply(Function1<ITerm, ITerm> f);

    IMessageContent withDefault(IMessageContent defaultContent);

    ITerm build();

    String toString(Function<ITerm, String> pp);

}