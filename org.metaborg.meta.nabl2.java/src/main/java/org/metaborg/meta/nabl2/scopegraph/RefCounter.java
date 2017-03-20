package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.util.collections.BagMultimap;
import org.metaborg.meta.nabl2.util.collections.HashBagMultimap;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class RefCounter<S, L> {

    private boolean complete;
    private final Multiset<S> openScopes;
    private final BagMultimap<S, L> openEdges;

    public RefCounter() {
        this.complete = false;
        this.openScopes = HashMultiset.create();
        this.openEdges = HashBagMultimap.create();
    }


    public void addOpenScope(S scope) {
        if(complete) {
            throw new IllegalStateException("Already complete.");
        }
        openScopes.add(scope);
    }

    public void addOpenEdge(S scope, L label) {
        if(complete) {
            throw new IllegalStateException("Already complete.");
        }
        openEdges.put(scope, label);
    }

    public void setComplete() {
        complete = true;
    }

    public void closeScope(S scope) {
        if(!openScopes.remove(scope)) {
            throw new IllegalStateException();
        }
    }

    public void closeEdge(S scope, L label) {
        if(!openEdges.remove(scope, label)) {
            throw new IllegalStateException();
        }
    }


    public boolean isScopeOpen(S scope) {
        return !complete || openScopes.contains(scope) || openEdges.containsKey(scope);
    }

    public void throwIfScopeClosed(S scope) throws IllegalArgumentException {
        if(isScopeOpen(scope)) {
            throw new IllegalArgumentException(scope + " is closed.");
        }
    }

    public boolean isEdgeOpen(S scope, L label) {
        return !complete || openScopes.contains(scope) || openEdges.containsEntry(scope, label);
    }

    public void throwIfEdgeClosed(S scope, L label) throws IllegalArgumentException {
        if(isEdgeOpen(scope, label)) {
            throw new IllegalArgumentException(scope + "/" + label + " is closed.");
        }
    }

}