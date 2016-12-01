package org.metaborg.meta.nabl2.solver;

import static org.metaborg.meta.nabl2.collections.Unit.unit;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.metaborg.meta.nabl2.collections.Unit;
import org.metaborg.meta.nabl2.constraints.namebinding.Decl;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint.CheckedCases;
import org.metaborg.meta.nabl2.constraints.namebinding.Resolve;
import org.metaborg.meta.nabl2.scopegraph.IResolutionResult;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NamebindingSolver implements ISolverComponent<INamebindingConstraint> {

    private static final long serialVersionUID = 7240605942212774052L;

    private final EqualitySolver unifier;

    private boolean complete = false;
    private final Set<INamebindingConstraint> defered = Sets.newHashSet();

    public NamebindingSolver(EqualitySolver unifier) {
        this.unifier = unifier;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public Unit add(INamebindingConstraint constraint) throws UnsatisfiableException {
        if (complete) {
            throw new IllegalStateException();
        }
        if (!solve(constraint)) {
            defered.add(constraint);
        }
        return unit;
    }

    @Override public boolean iterate() throws UnsatisfiableException {
        complete = true;
        Iterator<INamebindingConstraint> it = defered.iterator();
        boolean progress = false;
        while (it.hasNext()) {
            if (solve(it.next())) {
                progress |= true;
                it.remove();
            }
        }
        return progress;
    }

    @Override public void finish() throws UnsatisfiableException {
        for (INamebindingConstraint constraint : defered) {
            throw new UnsatisfiableException(constraint);
        }
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(INamebindingConstraint constraint) throws UnsatisfiableException {
        return constraint.matchOrThrow(CheckedCases.of(this::solve, this::solve));
    }

    private boolean solve(Decl c) throws UnsatisfiableException {
        ITerm declaration = unifier.find(c.getDeclaration());
        if (!declaration.isGround()) {
            return false;
        }
        ITerm scope = unifier.find(c.getScope());
        if (!scope.isGround()) {
            return false;
        }
        throw new UnsatisfiableException(c);
    }

    private boolean solve(Resolve c) {
        if (!complete) {
            return false;
        }
        ITerm reference = unifier.find(c.getReference());
        if (!reference.isGround()) {
            return false;
        }
        return resolve(reference).matchThrows(IResolutionResult.CheckedCases.of(decls -> {
            List<ITerm> declarations = Lists.newArrayList(decls);
            if (declarations.size() == 1) {
                unifier.unify(c.getDeclaration(), declarations.get(0));
            } else {
                throw new UnsatisfiableException();
            }
            return true;
        }, var -> {
            return false;
        }));
    }

    // ------------------------------------------------------------------------------------------------------//

    private IResolutionResult resolve(ITerm reference) {
        return null;
    }

}