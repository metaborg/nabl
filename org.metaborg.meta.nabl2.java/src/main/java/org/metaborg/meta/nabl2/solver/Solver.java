package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITermFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class Solver implements Serializable {

    private static final long serialVersionUID = -8700904608113913293L;

    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;

    private final Multimap<IStrategoTerm,String> errors;
    private final Multimap<IStrategoTerm,String> warnings;
    private final Multimap<IStrategoTerm,String> notes;

    private Solver(ITermFactory termFactory) {
        this.baseSolver = new BaseSolver();
        this.equalitySolver = new EqualitySolver(termFactory);
        this.namebindingSolver = new NamebindingSolver(equalitySolver);

        this.errors = HashMultimap.create();
        this.warnings = HashMultimap.create();
        this.notes = HashMultimap.create();
    }

    private void add(Iterable<IConstraint> constraints) throws UnsatisfiableException {
        for (IConstraint constraint : constraints) {
            try {
                constraint.matchOrThrow(CheckedCases.of(baseSolver::add, equalitySolver::add, namebindingSolver::add));
            } catch (UnsatisfiableException e) {
                errors.put(constraint.getOriginatingTerm().orElse(null), "Cannot satisfy '" + constraint + "'");
            }
        }
    }

    private void iterate() throws UnsatisfiableException {
        boolean progress;
        do {
            progress = false;
            progress |= baseSolver.iterate();
            progress |= equalitySolver.iterate();
            progress |= namebindingSolver.iterate();
        } while (progress);
    }

    private void finish() throws UnsatisfiableException {
        baseSolver.finish();
        equalitySolver.finish();
        namebindingSolver.finish();
    }

    public static Solution solve(Iterable<IConstraint> constraints, ITermFactory termFactory)
            throws UnsatisfiableException {
        Solver solver = new Solver(termFactory);
        solver.add(constraints);
        solver.iterate();
        solver.finish();
        return ImmutableSolution.of(solver.errors, solver.warnings, solver.notes);
    }

}