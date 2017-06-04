package org.metaborg.meta.nabl2.solver.components;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.constraints.nameresolution.INameResolutionConstraint;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.scopegraph.IScopeGraphConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.Scope;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolution;
import org.metaborg.meta.nabl2.solver.ISolver;
import org.metaborg.meta.nabl2.solver.ImmutableSeedResult;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.solver.messages.Messages;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.symbolic.ISymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;
import org.metaborg.meta.nabl2.util.Unit;
import org.metaborg.meta.nabl2.util.collections.IProperties;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CompositeComponent extends ASolver<IConstraint, ISolution> {

    private final ISolver<? super IAstConstraint, IProperties.Immutable<TermIndex, ITerm, ITerm>> astSolver;
    private final ISolver<? super IBaseConstraint, Unit> baseSolver;
    private final ISolver<? super IEqualityConstraint, IUnifier.Immutable> equalitySolver;
    private final ISolver<? super INameResolutionConstraint, IProperties.Immutable<Occurrence, ITerm, ITerm>> nameResolutionSolver;
    private final ISolver<? super IPolyConstraint, Unit> polySolver;
    private final ISolver<? super IRelationConstraint, IRelations.Immutable<ITerm>> relationSolver;
    private final ISolver<? super ISetConstraint, Unit> setSolver;
    private final ISolver<? super IScopeGraphConstraint, IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm>> scopeGraphSolver;
    private final ISolver<? super ISymbolicConstraint, ISymbolicConstraints> symbolicSolver;

    private final List<ISolver<?, ?>> components = Lists.newArrayList();

    public CompositeComponent(SolverCore core,
            // @formatter:off
            ISolver<? super IAstConstraint, IProperties.Immutable<TermIndex,ITerm,ITerm>> astSolver,
            ISolver<? super IBaseConstraint, Unit> baseSolver,
            ISolver<? super IEqualityConstraint, IUnifier.Immutable> equalitySolver,
            ISolver<? super INameResolutionConstraint, IProperties.Immutable<Occurrence,ITerm,ITerm>> nameResolutionSolver,
            ISolver<? super IPolyConstraint, Unit> polySolver,
            ISolver<? super IRelationConstraint, IRelations.Immutable<ITerm>> relationSolver,
            ISolver<? super ISetConstraint, Unit> setSolver,
            ISolver<? super IScopeGraphConstraint, IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm>> scopeGraphSolver,
            ISolver<? super ISymbolicConstraint, ISymbolicConstraints> symbolicSolver
            // @formatter:on
    ) {
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

    @Override public SeedResult seed(ISolution solution, IMessageInfo message) throws InterruptedException {
        Set<IConstraint> constraints = Sets.newHashSet(solution.constraints());
        Messages.Transient messages = Messages.Transient.of();
        seed(astSolver, solution.astProperties(), constraints, messages, message);
        seed(equalitySolver, solution.unifier(), constraints, messages, message);
        seed(relationSolver, solution.relations(), constraints, messages, message);
        seed(nameResolutionSolver, solution.declProperties(), constraints, messages, message);
        seed(scopeGraphSolver, solution.scopeGraph(), constraints, messages, message);
        seed(symbolicSolver, solution.symbolic(), constraints, messages, message);
        return ImmutableSeedResult.builder()
                // @formatter:off
                .messages(messages.freeze())
                .constraints(constraints)
                // @formatter:on
                .build();
    }

    private <R> void seed(ISolver<?, R> component, R solution, Set<IConstraint> constraints,
            Messages.Transient messages, IMessageInfo message) throws InterruptedException {
        SeedResult result = component.seed(solution, message);
        constraints.addAll(result.constraints());
        messages.addAll(result.messages());
    }

    @Override public Optional<SolveResult> solve(IConstraint constraint) throws InterruptedException {
        final Optional<SolveResult> result = constraint.matchOrThrow(IConstraint.CheckedCases.of(astSolver::solve,
                baseSolver::solve, equalitySolver::solve, scopeGraphSolver::solve, nameResolutionSolver::solve,
                relationSolver::solve, setSolver::solve, symbolicSolver::solve, polySolver::solve));
        return result;
    }

    @Override public boolean update() throws InterruptedException {
        boolean progress;
        do {
            progress = false;
            for(ISolver<?, ?> component : components) {
                component.getTimer().start();
                try {
                    progress |= component.update();
                } finally {
                    component.getTimer().stop();
                }
            }
        } while(progress);
        return true;
    }

    public ISolution finish() {
        IProperties.Immutable<TermIndex, ITerm, ITerm> astProperties = astSolver.finish();
        baseSolver.finish();
        IUnifier.Immutable unifier = equalitySolver.finish();
        IProperties.Immutable<Occurrence, ITerm, ITerm> declProperties = nameResolutionSolver.finish();
        polySolver.finish();
        setSolver.finish();
        IEsopScopeGraph.Immutable<Scope, Label, Occurrence, ITerm> scopeGraph = scopeGraphSolver.finish();
        ISymbolicConstraints symbolicConstraints = symbolicSolver.finish();

        return ImmutableSolution.builder()
                // @formatter:off
                .config(config())
                .astProperties(astProperties)
                .unifier(unifier)
                .declProperties(declProperties)
                .scopeGraph(scopeGraph)
                .symbolic(symbolicConstraints)
                // @formatter:on
                .build();
    }

}