package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;
import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.IConstraint.CheckedCases;
import org.metaborg.meta.nabl2.terms.ITermFactory;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Lists;

public class Solver implements Serializable {

    private static final long serialVersionUID = -8700904608113913293L;

    private final BaseSolver baseSolver;
    private final EqualitySolver equalitySolver;
    private final NamebindingSolver namebindingSolver;

    private final List<Message> errors;
    private final List<Message> warnings;
    private final List<Message> notes;

    private Solver(ITermFactory termFactory) {
        this.baseSolver = new BaseSolver();
        this.equalitySolver = new EqualitySolver(termFactory);
        this.namebindingSolver = new NamebindingSolver(equalitySolver);

        this.errors = Lists.newArrayList();
        this.warnings = Lists.newArrayList();
        this.notes = Lists.newArrayList();
    }

    private void add(Iterable<IConstraint> constraints) throws UnsatisfiableException {
        for (IConstraint constraint : constraints) {
            try {
                constraint.matchThrows(CheckedCases.of(baseSolver::add, equalitySolver::add, namebindingSolver::add));
            } catch (UnsatisfiableException e) {
                // add error
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

    public static ISolution solve(Iterable<IConstraint> constraints, ITermFactory termFactory)
            throws UnsatisfiableException {
        Solver solver = new Solver(termFactory);
        solver.add(constraints);
        solver.iterate();
        solver.finish();
        return new ISolution() {

            private static final long serialVersionUID = 1L;

            @Override public Iterable<Message> getWarnings() {
                return Iterables2.empty();
            }

            @Override public Iterable<Message> getNotes() {
                return Iterables2.empty();
            }

            @Override public Iterable<Message> getErrors() {
                return Iterables2.empty();
            }
        };
    }

}