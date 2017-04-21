package org.metaborg.meta.nabl2.unification;

import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.terms.ITerm;

public class UnificationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final MessageContent messageContent;

    public UnificationException(ITerm term1, ITerm term2) {
        super("Cannot unify " + term1 + " with " + term2);
        this.messageContent = MessageContent.builder()
            .append("Cannot unify ")
            .append(term1)
            .append(" with ")
            .append(term2)
            .build();
    }

    public MessageContent getMessageContent() {
        return messageContent;
    }
    
}