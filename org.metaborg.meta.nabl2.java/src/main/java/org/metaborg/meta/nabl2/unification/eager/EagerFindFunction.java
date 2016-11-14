package org.metaborg.meta.nabl2.unification.eager;

import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.IListTerm;
import org.metaborg.meta.nabl2.terms.INilTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunctionWithException;
import org.metaborg.meta.nabl2.terms.ITermOp;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ITupleTerm;
import org.metaborg.meta.nabl2.terms.generic.ImmutableApplTerm;
import org.metaborg.meta.nabl2.terms.generic.ImmutableConsTerm;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTupleTerm;
import org.metaborg.meta.nabl2.unification.UnificationException;

import com.google.common.collect.ImmutableList;

final class EagerFindFunction implements ITermFunctionWithException<EagerFindResult> {

    private final EagerTermUnifier unifier;

    EagerFindFunction(EagerTermUnifier unifier) {
        this.unifier = unifier;
    }

    @Override public EagerFindResult apply(ITermVar term) throws UnificationException {
    }

    @Override public EagerFindResult apply(ITermOp term) throws UnificationException {
        if (unifier.termReps.containsKey(term)) {
            EagerFindResult result = unifier.termReps.get(term).apply(this);
            return new EagerFindResult(result.rep(),
                    new EagerTermUnifier(result.unifier().varReps, result.unifier().termReps.put(term, result.rep())));
        } else if (term.isGround()) {
            // TODO Try reduction
            return new EagerFindResult(term, unifier);
        } else {
            return new EagerFindResult(term, unifier);
        }
    }

    @Override public EagerFindResult apply(IApplTerm term) throws UnificationException {
        if (term.isGround()) {
            return new EagerFindResult(term, unifier);
        } else if (unifier.termReps.containsKey(term)) {
            EagerFindResult result = unifier.termReps.get(term).apply(this);
            return new EagerFindResult(result.rep(),
                    new EagerTermUnifier(result.unifier().varReps, result.unifier().termReps.put(term, result.rep())));
        } else {
            ImmutableList.Builder<ITerm> argBuilder = ImmutableList.builder();
            EagerTermUnifier localUnifier = unifier;
            for (ITerm arg : term.getArgs()) {
                if (arg.isGround()) {
                    argBuilder.add(arg);
                } else {
                    EagerFindResult result = localUnifier.find(arg);
                    argBuilder.add(result.rep());
                    localUnifier = result.unifier();
                }
            }
            boolean someArgsUpdated = localUnifier != unifier;
            if (someArgsUpdated) {
                IApplTerm newTerm = ImmutableApplTerm.of(term.getOp(), argBuilder.build());
                return new EagerFindResult(newTerm,
                        new EagerTermUnifier(unifier.varReps, unifier.termReps.put(term, newTerm)));
            } else {
                return new EagerFindResult(term, localUnifier);
            }
        }
    }

    @Override public EagerFindResult apply(ITupleTerm term) throws UnificationException {
        if (term.isGround()) {
            return new EagerFindResult(term, unifier);
        } else if (unifier.termReps.containsKey(term)) {
            EagerFindResult result = unifier.termReps.get(term).apply(this);
            return new EagerFindResult(result.rep(),
                    new EagerTermUnifier(result.unifier().varReps, result.unifier().termReps.put(term, result.rep())));
        } else {
            ImmutableList.Builder<ITerm> argBuilder = ImmutableList.builder();
            EagerTermUnifier localUnifier = unifier;
            for (ITerm arg : term.getArgs()) {
                if (arg.isGround()) {
                    argBuilder.add(arg);
                } else {
                    EagerFindResult result = localUnifier.find(arg);
                    argBuilder.add(result.rep());
                    localUnifier = result.unifier();
                }
            }
            boolean someArgsUpdated = localUnifier != unifier;
            if (someArgsUpdated) {
                ITupleTerm newTerm = ImmutableTupleTerm.of(argBuilder.build());
                return new EagerFindResult(newTerm,
                        new EagerTermUnifier(unifier.varReps, unifier.termReps.put(term, newTerm)));
            } else {
                return new EagerFindResult(term, localUnifier);
            }
        }
    }

    @Override public EagerFindResult apply(IStringTerm term) throws UnificationException {
        return new EagerFindResult(term, unifier);
    }

    @Override public EagerFindResult apply(IIntTerm term) throws UnificationException {
        return new EagerFindResult(term, unifier);
    }

    @Override public EagerFindResult apply(IConsTerm term) throws UnificationException {
        if (term.isGround()) {
            return new EagerFindResult(term, unifier);
        } else if (unifier.termReps.containsKey(term)) {
            EagerFindResult result = unifier.termReps.get(term).apply(this);
            return new EagerFindResult(result.rep(),
                    new EagerTermUnifier(result.unifier().varReps, result.unifier().termReps.put(term, result.rep())));
        } else {
            EagerTermUnifier localUnifier = unifier;
            ITerm head = term.getHead();
            if (!head.isGround()) {
                EagerFindResult result = localUnifier.find(head);
                head = result.rep();
                localUnifier = result.unifier();
            }
            IListTerm tail = term.getTail();
            if (!tail.isGround()) {
                EagerFindResult result = unifier.find(tail);
                tail = (IListTerm) result.rep();
                localUnifier = result.unifier();
            }
            boolean someArgsUpdated = localUnifier != unifier;
            if (someArgsUpdated) {
                IConsTerm newTerm = ImmutableConsTerm.of(head, tail);
                return new EagerFindResult(newTerm,
                        new EagerTermUnifier(unifier.varReps, unifier.termReps.put(term, newTerm)));
            } else {
                return new EagerFindResult(term, localUnifier);
            }
        }
    }

    @Override public EagerFindResult apply(INilTerm term) throws UnificationException {
        return new EagerFindResult(term, unifier);
    }

}