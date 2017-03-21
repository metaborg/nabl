package org.metaborg.meta.nabl2.scopegraph;

import java.util.Collection;

import org.metaborg.meta.nabl2.util.collections.BagMultimap;
import org.metaborg.meta.nabl2.util.collections.HashBagMultimap;

public class OpenCounter<S, L> {

    private boolean complete;
    private final BagMultimap<S, L> open;

    public OpenCounter() {
        this.complete = false;
        this.open = HashBagMultimap.create();
    }


    public void add(S scope, L label) {
        if(complete) {
            throw new IllegalStateException("Already complete.");
        }
        open.put(scope, label);
    }

    public void addAll(S scope, Collection<L> labels) {
        if(complete) {
            throw new IllegalStateException("Already complete.");
        }
        open.putAll(scope, labels);
    }

    public void setComplete() {
        complete = true;
    }

    public void remove(S scope, L label) {
        if(!open.remove(scope, label)) {
            throw new IllegalStateException();
        }
    }

    public void removeAll(S scope, Collection<L> labels) {
        if(!open.removeAll(scope, labels)) {
            throw new IllegalStateException();
        }
    }

    public boolean isOpen(S scope, L label) {
        return !complete || open.containsEntry(scope, label);
    }

    public void throwIfClosed(S scope, L label) throws IllegalArgumentException {
        if(isOpen(scope, label)) {
            throw new IllegalArgumentException(scope + "/" + label + " is closed.");
        }
    }

}