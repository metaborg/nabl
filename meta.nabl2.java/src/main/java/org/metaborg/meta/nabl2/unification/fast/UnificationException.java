package org.metaborg.meta.nabl2.unification.fast;

import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;

public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ITerm left;
    private final ITerm right;
    private final MessageContent messageContent;

    public UnificationException(ITerm left, ITerm right) {
        super("Cannot unify " + left + " with " + right);
        this.left = left;
        this.right = right;
        this.messageContent =
                MessageContent.builder().append("Cannot unify ").append(left).append(" with ").append(right).build();
    }

    public ITerm getLeft() {
        return left;
    }

    public ITerm getRight() {
        return right;
    }

    public MessageContent getMessageContent() {
        return messageContent;
    }

}