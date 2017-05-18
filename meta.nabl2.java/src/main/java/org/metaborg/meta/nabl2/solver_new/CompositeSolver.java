package org.metaborg.meta.nabl2.solver_new;

import java.util.List;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;

import com.google.common.collect.Lists;

public class CompositeSolver extends ASolver<IConstraint, Void> {

    private final ASolver<? super IAstConstraint, ?> astSolver;
    private final ASolver<? super IBaseConstraint, ?> baseSolver;
    private final ASolver<? super IEqualityConstraint, ?> equalitySolver;
    private final ASolver<? super INameResolutionConstraint, ?> nameResolutionSolver;
    private final ASolver<? super IPolyConstraint, ?> polySolver;
    private final ASolver<? super IRelationConstraint, ?> relationSolver;
    private final ASolver<? super ISetConstraint, ?> setSolver;
    private final ASolver<? super IScopeGraphConstraint, ?> scopeGraphSolver;
    private final ASolver<? super ISymbolicConstraint, ?> symbolicSolver;

    private final List<ASolver<?, ?>> components = Lists.newArrayList();

    public CompositeSolver(SolverCore core, ASolver<? super IAstConstraint, ?> astSolver,
            ASolver<? super IBaseConstraint, ?> baseSolver, ASolver<? super IEqualityConstraint, ?> equalitySolver,
            ASolver<? super INameResolutionConstraint, ?> nameResolutionSolver,
            ASolver<? super IPolyConstraint, ?> polySolver, ASolver<? super IRelationConstraint, ?> relationSolver,
            ASolver<? super ISetConstraint, ?> setSolver, ASolver<? super IScopeGraphConstraint, ?> scopeGraphSolver,
            ASolver<? super ISymbolicConstraint, ?> symbolicSolver) {
        super(core);
        components.add(this.astSolver = astSolver);
        components.add(this.baseSolver = baseSolver);
        components.add(this.equalitySolver = equalitySolver);
        components.add(this.nameResolutionSolver = nameResolutionSolver);
        components.add(this.polySolver = polySolver);
        components.add(this.relationSolver = relationSolver);
        components.add(this.setSolver = setSolver);
        components.add(this.scopeGraphSolver = scopeGraphSolver);
        components.add(this.symbolicSolver = symbolicSolver);
    }

    public boolean addAll(Iterable<? extends IConstraint> constraints) throws InterruptedException {
        boolean progress = false;
        for(IConstraint constraint : constraints) {
            progress |= add(constraint);
        }
        return progress;
    }

    @Override public boolean add(IConstraint constraint) throws InterruptedException {
        return constraint.matchOrThrow(IConstraint.CheckedCases.of(astSolver::add, baseSolver::add, equalitySolver::add,
                scopeGraphSolver::add, nameResolutionSolver::add, relationSolver::add, setSolver::add,
                symbolicSolver::add, polySolver::add));
    }

    @Override public boolean iterate() throws InterruptedException {
        boolean progress;
        do {
            progress = false;
            for(ASolver<?, ?> component : components) {
                throwIfCancelled();
                component.getTimer().start();
                try {
                    progress |= component.iterate();
                } finally {
                    component.getTimer().stop();
                }
            }
        } while(progress);
        return true;
    }

    public Void finish() {
        throw new IllegalStateException(
                "Cannot call finish on CompositeSolver, must be called on the individual components.");
    }

}