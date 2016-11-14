package org.metaborg.meta.nabl2.unification.lazy;

import org.metaborg.meta.nabl2.terms.ATermFunction;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermOp;
import org.metaborg.meta.nabl2.terms.ITermVar;

final class LazyFindFunction extends ATermFunction<LazyFindResult> {

    private final LazyTermUnifier unifier;

    LazyFindFunction(LazyTermUnifier unifier) {
        this.unifier = unifier;
    }

    @Override public LazyFindResult apply(ITermVar termVar) {
        if (unifier.varReps.containsKey(termVar)) {
            LazyFindResult result = unifier.varReps.get(termVar).apply(this);
            return new LazyFindResult(result.rep(),
                    new LazyTermUnifier(result.unifier().varReps.put(termVar, result.rep()), result.unifier().opReps));
        } else {
            return new LazyFindResult(termVar, unifier);
        }
    }

    @Override public LazyFindResult apply(ITermOp termOp) {
        if (unifier.opReps.containsKey(termOp)) {
            LazyFindResult result = unifier.opReps.get(termOp).apply(this);
            return new LazyFindResult(result.rep(),
                    new LazyTermUnifier(result.unifier().varReps, result.unifier().opReps.put(termOp, result.rep())));
        } else {
            return new LazyFindResult(termOp, unifier);
        }
    }

    @Override public LazyFindResult defaultApply(ITerm term) {
        return new LazyFindResult(term, unifier);
    }

}