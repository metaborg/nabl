package org.metaborg.meta.nabl2.unification.eager;

import java.util.Iterator;
import java.util.List;

import org.metaborg.meta.nabl2.terms.ATermFunction;
import org.metaborg.meta.nabl2.terms.IApplTerm;
import org.metaborg.meta.nabl2.terms.IConsTerm;
import org.metaborg.meta.nabl2.terms.IIntTerm;
import org.metaborg.meta.nabl2.terms.INilTerm;
import org.metaborg.meta.nabl2.terms.IStringTerm;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunctionWithException;
import org.metaborg.meta.nabl2.terms.ITermOp;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.ITupleTerm;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermPair;
import org.metaborg.meta.nabl2.terms.generic.TermPair;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.util.iterators.CompoundIterable;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

final class EagerUnifyFunction implements ITermFunctionWithException<EagerUnifyResult> {

    private final EagerTermUnifier unifier;
    private final ITerm second;

    public EagerUnifyFunction(EagerTermUnifier unifier, ITerm second) {
        this.unifier = unifier;
        this.second = second;
    }

    // ***** Var & Op *****

    @Override public EagerUnifyResult apply(final ITermVar first) throws UnificationException {
        return EagerUnifyResult.result(new EagerTermUnifier(unifier.varReps.put(first, second), unifier.termReps));
    }

    public EagerUnifyResult apply(ITermOp first) throws UnificationException {
        return null;
    };

    private class VarVisitor<T extends ITerm> extends ATermFunction<EagerUnifyResult> {

        protected final T first;

        public VarVisitor(T first) {
            this.first = first;
        }

        @Override public EagerUnifyResult apply(ITermVar second) {
            return EagerUnifyResult.result(new EagerTermUnifier(unifier.varReps.put(second, first), unifier.termReps));
        }

        @Override public EagerUnifyResult apply(ITermOp second) {
            return null;
        }

        @Override public EagerUnifyResult defaultApply(ITerm second) {
            return EagerUnifyResult.resultWithConflict(unifier, ImmutableTermPair.of(first, second));
        }

    }

    // ***** String *****

    @Override public EagerUnifyResult apply(final IStringTerm first) throws UnificationException {
        return second.apply(new StringVisitor(first));
    }

    private class StringVisitor extends VarVisitor<IStringTerm> {

        public StringVisitor(IStringTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(IStringTerm second) {
            if (!first.equals(second)) {
                return EagerUnifyResult.resultWithConflict(unifier, ImmutableTermPair.of(first, second));
            }
            return EagerUnifyResult.result(unifier);
        }
    }

    // ***** Int *****

    @Override public EagerUnifyResult apply(final IIntTerm first) throws UnificationException {
        return second.apply(new IntVisitor(first));
    }

    private class IntVisitor extends VarVisitor<IIntTerm> {

        public IntVisitor(IIntTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(IIntTerm second) {
            if (!first.equals(second)) {
                return EagerUnifyResult.resultWithConflict(unifier, ImmutableTermPair.of(first, second));
            }
            return EagerUnifyResult.result(unifier);
        }
    }

    // ***** Appl *****

    public EagerUnifyResult apply(IApplTerm first) throws UnificationException {
        return second.apply(new ApplVisitor(first));
    };

    private class ApplVisitor extends VarVisitor<IApplTerm> {

        public ApplVisitor(IApplTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(IApplTerm second) {
            if (!first.getOp().equals(second.getOp())) {
                return EagerUnifyResult.resultWithConflict(unifier, ImmutableTermPair.of(first, second));
            }
            return applyArgs(first, first.getArgs(), second, second.getArgs());
        }

    }

    // ***** List *****

    public EagerUnifyResult apply(IConsTerm first) throws UnificationException {
        return second.apply(new ConsVisitor(first));
    };

    private class ConsVisitor extends VarVisitor<IConsTerm> {

        public ConsVisitor(IConsTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(IConsTerm second) {
            EagerUnifyResult heads = unifier.unify(first.getHead(), second.getHead());
            return heads.unifier().unify(first.getTail(), second.getTail());
        }

    }

    public EagerUnifyResult apply(INilTerm first) throws UnificationException {
        return second.apply(new NilVisitor(first));
    };

    private class NilVisitor extends VarVisitor<INilTerm> {

        public NilVisitor(INilTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(INilTerm second) {
            return EagerUnifyResult.result(unifier);
        }

    }

    // ***** Tuple*****

    public EagerUnifyResult apply(ITupleTerm first) throws UnificationException {
        return second.apply(new TupleVisitor(first));
    };

    private class TupleVisitor extends VarVisitor<ITupleTerm> {

        public TupleVisitor(ITupleTerm first) {
            super(first);
        }

        @Override public EagerUnifyResult apply(ITupleTerm second) {
            return applyArgs(first, first.getArgs(), second, second.getArgs());
        }

    }

    // ***** WithArgs *****

    private EagerUnifyResult applyArgs(final ITerm first, final Iterable<ITerm> args1, final ITerm second,
            final Iterable<ITerm> args2) throws UnificationException {
        if (Iterables.size(args1) != Iterables.size(args2)) {
            return EagerUnifyResult.resultWithConflict(unifier, ImmutableTermPair.of(first, second));
        }
        List<Iterable<? extends TermPair>> conflicts = Lists.newLinkedList();
        List<Iterable<? extends TermPair>> defers = Lists.newLinkedList();
        EagerTermUnifier localUnifier = unifier;
        Iterator<ITerm> it1 = args1.iterator();
        Iterator<ITerm> it2 = args2.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            ITerm arg1 = it1.next();
            ITerm arg2 = it2.next();
            EagerUnifyResult result = localUnifier.unify(arg1, arg2);
            if (result == null) {
                defers.add(Iterables2.singleton(ImmutableTermPair.of(first, second)));
            } else {
                conflicts.add(result.conflicts());
                defers.add(result.defers());
                localUnifier = result.unifier();
            }
        }
        return EagerUnifyResult.result(localUnifier, new CompoundIterable<? extends TermPair>(conflicts), new CompoundIterable<? extends TermPair>(defers));
    }

}