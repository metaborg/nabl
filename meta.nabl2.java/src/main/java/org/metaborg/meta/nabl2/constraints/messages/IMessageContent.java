package org.metaborg.meta.nabl2.constraints.messages;

import java.util.function.Function;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.functions.Function1;

public interface IMessageContent {

    IMessageContent apply(Function1<ITerm, ITerm> f);

    IMessageContent withDefault(IMessageContent defaultContent);

    ITerm build();

    String toString(Function<ITerm, String> pp);

}