package org.metaborg.meta.nabl2.solver.components;

import static org.metaborg.meta.nabl2.util.Unit.unit;

import java.util.Set;

import org.metaborg.meta.nabl2.constraints.equality.CEqual;
import org.metaborg.meta.nabl2.constraints.equality.CInequal;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.solver.Solver;
import org.metaborg.meta.nabl2.solver.SolverComponent;
import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.UnificationException;
import org.metaborg.meta.nabl2.util.Unit;

import com.google.common.collect.Sets;

public class EqualitySolver extends SolverComponent<IEqualityConstraint> {

    private final Set<CInequal> defered;

    public EqualitySolver(Solver solver) {
        super(solver);
        this.defered = Sets.newHashSet();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override protected Unit doAdd(IEqualityConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::add, this::add));
    }

    @Override protected boolean doIterate() throws UnsatisfiableException, InterruptedException {
        return doIterate(defered, this::solve);
    }

    @Override protected Set<? extends IEqualityConstraint> doFinish(IMessageInfo messageInfo) {
        return defered;
    }

    // ------------------------------------------------------------------------------------------------------//

    private Unit add(CEqual constraint) throws UnsatisfiableException {
        solve(constraint);
        work();
        return unit;
    }

    private Unit add(CInequal constraint) throws UnsatisfiableException {
        if(!solve(constraint)) {
            defered.add(constraint);
        } else {
            work();
        }
        return unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(CEqual constraint) throws UnsatisfiableException {
        ITerm left = unifier().find(constraint.getLeft());
        ITerm right = unifier().find(constraint.getRight());
        try {
            unifier().unify(left, right);
        } catch(UnificationException ex) {
            MessageContent content = MessageContent.builder().append("Cannot unify ").append(left).append(" with ")
                    .append(right).build();
            throw new UnsatisfiableException(constraint.getMessageInfo().withDefaultContent(content));
        }
        return true;
    }

    private boolean solve(CInequal constraint) throws UnsatisfiableException {
        ITerm left = unifier().find(constraint.getLeft());
        ITerm right = unifier().find(constraint.getRight());
        if(left.equals(right)) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but both resolve to ")
                    .append(constraint.getLeft()).build();
            throw new UnsatisfiableException(constraint.getMessageInfo().withDefaultContent(content));
        }
        return !unifier().canUnify(left, right);
    }

}