package org.metaborg.meta.nabl2.unification.eager;

import static org.metaborg.meta.nabl2.terms.generic.TermPair.EMPTY;

import org.metaborg.meta.nabl2.terms.generic.TermPair;
import org.metaborg.meta.nabl2.unification.IUnifyResult;
import org.metaborg.util.iterators.Iterables2;

final class EagerUnifyResult implements IUnifyResult {

    private final EagerTermUnifier unifier;
    private final Iterable<? extends TermPair> defers;

    private EagerUnifyResult(EagerTermUnifier unifier, Iterable<? extends TermPair> defers) {
        this.unifier = unifier;
        this.defers = defers;
    }

    @Override public EagerTermUnifier unifier() {
        return unifier;
    }

    @Override public Iterable<? extends TermPair> defers() {
        return defers;
    }

    public static EagerUnifyResult result(EagerTermUnifier unifier) {
        return new EagerUnifyResult(unifier, EMPTY);
    }

    public static EagerUnifyResult resultWithDefer(EagerTermUnifier unifier, TermPair defer) {
        return new EagerUnifyResult(unifier, Iterables2.singleton(defer));
    }

    public static EagerUnifyResult resultWithDefers(EagerTermUnifier unifier, Iterable<? extends TermPair> defers) {
        return new EagerUnifyResult(unifier, defers);
    }

}