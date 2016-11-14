package org.metaborg.meta.nabl2.unification.lazy;

import java.util.List;

import org.metaborg.meta.nabl2.unification.IPrimitiveTerm;
import org.metaborg.meta.nabl2.unification.ITerm;
import org.metaborg.meta.nabl2.unification.ITermFunction;
import org.metaborg.meta.nabl2.unification.terms.ATermFunction;
import org.metaborg.meta.nabl2.unification.terms.IApplTerm;
import org.metaborg.meta.nabl2.unification.terms.IConsTerm;
import org.metaborg.meta.nabl2.unification.terms.INilTerm;
import org.metaborg.meta.nabl2.unification.terms.ITermOp;
import org.metaborg.meta.nabl2.unification.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.terms.ITermWithArgs;
import org.metaborg.meta.nabl2.unification.terms.ITupleTerm;
import org.metaborg.meta.nabl2.unification.terms.TermPair;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

final class LazyUnifyFunction implements ITermFunction<LazyUnifyResult> {

    private final LazyTermUnifier unifier;
    private final ITerm second;

    public LazyUnifyFunction(LazyTermUnifier unifier, ITerm second) {
        this.unifier = unifier;
        this.second = second;
    }

    // ***** Var & Op *****

    @Override public LazyUnifyResult apply(final ITermVar first) {
        return LazyUnifyResult.result(new LazyTermUnifier(unifier.varReps.put(first, second), unifier.opReps));
    }

    public LazyUnifyResult apply(final ITermOp first) {
        return null;
    };

    private class VarVisitor<T extends ITerm> extends ATermFunction<LazyUnifyResult> {

        protected final T first;

        public VarVisitor(T first) {
            this.first = first;
        }

        @Override public LazyUnifyResult apply(ITermVar second) {
            return LazyUnifyResult.result(new LazyTermUnifier(unifier.varReps.put(second, first), unifier.opReps));
        }

        @Override public LazyUnifyResult apply(ITermOp second) {
            return null;
        }

        @Override public LazyUnifyResult defaultApply(ITerm second) {
            return LazyUnifyResult.resultWithConflict(unifier, TermPair.of(first, second));
        }

    }

    // ***** Primitive *****

    @Override public LazyUnifyResult apply(final IPrimitiveTerm first) {
        return second.apply(new PrimitiveVisitor(first));
    }

    private class PrimitiveVisitor extends VarVisitor<IPrimitiveTerm> {

        public PrimitiveVisitor(IPrimitiveTerm first) {
            super(first);
        }

        @Override public LazyUnifyResult apply(IPrimitiveTerm second) {
            if (!first.equals(second)) {
                return LazyUnifyResult.resultWithConflict(unifier, TermPair.of(first, second));
            }
            return LazyUnifyResult.result(unifier);
        }
    }

    // ***** WithArgs *****

    private LazyUnifyResult visitArgs(ITermWithArgs first, ITermWithArgs second) {
        final ImmutableList<ITerm> args1 = first.getArgs();
        final ImmutableList<ITerm> args2 = second.getArgs();
        if (args1.size() != args2.size()) {
            return LazyUnifyResult.resultWithConflict(unifier, TermPair.of(first, second));
        }
        List<Iterable<TermPair>> conflicts = Lists.newLinkedList();
        List<Iterable<TermPair>> defers = Lists.newLinkedList();
        LazyTermUnifier localUnifier = unifier;
        for (int i = 0; i < args1.size(); i++) {
            LazyUnifyResult result = localUnifier.unify(args1.get(i), args2.get(i));
            if (result == null) {
                defers.add(Iterables2.singleton(TermPair.of(first, second)));
            } else {
                conflicts.add(result.conflicts());
                defers.add(result.defers());
                localUnifier = result.unifier();
            }
        }
        return LazyUnifyResult.result(localUnifier, Iterables2.fromConcat(conflicts), Iterables2.fromConcat(defers));
    }

    // ***** Appl *****

    public LazyUnifyResult apply(IApplTerm first) {
        return second.apply(new ApplVisitor(first));
    };

    private class ApplVisitor extends VarVisitor<IApplTerm> {

        public ApplVisitor(IApplTerm first) {
            super(first);
        }

        @Override public LazyUnifyResult apply(IApplTerm second) {
            if (!first.getOp().equals(second.getOp())) {
                return LazyUnifyResult.resultWithConflict(unifier, TermPair.of(first, second));
            }
            return visitArgs(first, second);
        }
    }

    // ***** List *****

    public LazyUnifyResult apply(IConsTerm first) {
        return second.apply(new ConsVisitor(first));
    };

    private class ConsVisitor extends VarVisitor<IConsTerm> {

        public ConsVisitor(IConsTerm first) {
            super(first);
        }

        @Override public LazyUnifyResult apply(IConsTerm second) {
            LazyUnifyResult heads = unifier.unify(first.getHead(), second.getHead());
            return heads.unifier().unify(first.getTail(), second.getTail());
        }

    }

    public LazyUnifyResult apply(INilTerm first) {
        return second.apply(new NilVisitor(first));
    };

    private class NilVisitor extends VarVisitor<INilTerm> {

        public NilVisitor(INilTerm first) {
            super(first);
        }

        @Override public LazyUnifyResult apply(INilTerm second) {
            return LazyUnifyResult.result(unifier);
        }

    }

    // ***** Tuple*****

    public LazyUnifyResult apply(ITupleTerm first) {
        return second.apply(new TupleVisitor(first));
    };

    private class TupleVisitor extends VarVisitor<ITupleTerm> {

        public TupleVisitor(ITupleTerm first) {
            super(first);
        }

        @Override public LazyUnifyResult apply(ITupleTerm second) {
            return visitArgs(first, second);
        }
    }

}