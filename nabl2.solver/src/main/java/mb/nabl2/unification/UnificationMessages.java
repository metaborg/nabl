package mb.nabl2.unification;

import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.terms.ITerm;

public class UnificationMessages {

    public static MessageContent getError(ITerm left, ITerm right) {
        return MessageContent.builder().append("Cannot unify ").append(left).append(" with ").append(right).build();
    }

}