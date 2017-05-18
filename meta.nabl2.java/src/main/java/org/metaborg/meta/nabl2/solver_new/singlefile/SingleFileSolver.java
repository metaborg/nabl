package org.metaborg.meta.nabl2.solver_new.singlefile;

import org.metaborg.meta.nabl2.constraints.IConstraint;
import org.metaborg.meta.nabl2.relations.terms.Relations;
import org.metaborg.meta.nabl2.solver.ImmutableSolution;
import org.metaborg.meta.nabl2.solver.Solution;
import org.metaborg.meta.nabl2.solver.SolverConfig;
import org.metaborg.meta.nabl2.solver.SolverException;
import org.metaborg.meta.nabl2.solver.SymbolicConstraints;
import org.metaborg.meta.nabl2.solver_new.CompositeSolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.solver_new.SolverCore.CoreResult;
import org.metaborg.meta.nabl2.solver_new.components.AstSolver;
import org.metaborg.meta.nabl2.solver_new.components.AstSolver.AstResult;
import org.metaborg.meta.nabl2.solver_new.components.BaseSolver;
import org.metaborg.meta.nabl2.solver_new.components.DeferringSolver;
import org.metaborg.meta.nabl2.solver_new.components.DeferringSolver.DeferringResult;
import org.metaborg.meta.nabl2.solver_new.components.EqualitySolver;
import org.metaborg.meta.nabl2.solver_new.components.EqualitySolver.EqualityResult;
import org.metaborg.meta.nabl2.solver_new.components.NameResolutionSolver;
import org.metaborg.meta.nabl2.solver_new.components.NameResolutionSolver.NameResolutionResult;
import org.metaborg.meta.nabl2.solver_new.components.PolySolver;
import org.metaborg.meta.nabl2.solver_new.components.RelationSolver;
import org.metaborg.meta.nabl2.solver_new.components.RelationSolver.RelationResult;
import org.metaborg.meta.nabl2.solver_new.components.ScopeGraphSolver;
import org.metaborg.meta.nabl2.solver_new.components.ScopeGraphSolver.ScopeGraphResult;
import org.metaborg.meta.nabl2.solver_new.components.SetSolver;
import org.metaborg.meta.nabl2.solver_new.components.SetSolver.SetResult;
import org.metaborg.meta.nabl2.solver_new.components.SymbolicSolver;
import org.metaborg.meta.nabl2.solver_new.components.ThrowingSolver;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;

import com.google.common.collect.Sets;

import io.usethesource.capsule.Set;

public class SingleFileSolver {

    private final SolverConfig config;

    public SingleFileSolver(SolverConfig config) {
        this.config = config;
    }

    public Solution solve(Iterable<IConstraint> constraints, Function1<String, ITermVar> fresh, ICancel cancel,
            IProgress progress) throws SolverException, InterruptedException {

        try {
            final java.util.Set<IConstraint> unsolved = Sets.newHashSet();

            final CoreResult coreResult1;
            final EqualityResult equalityResult1;
            final ScopeGraphResult scopeGraphResult1;
            final DeferringResult<IConstraint> deferred1;
            {
                final SolverCore core = new SolverCore(config, fresh, cancel, progress);
                final DeferringSolver<IConstraint> defer = new DeferringSolver<>(core);
                final BaseSolver baseSolver = new BaseSolver(core);
                final EqualitySolver equalitySolver = new EqualitySolver(core);
                final ScopeGraphSolver scopeGraphSolver = new ScopeGraphSolver(core);
                final CompositeSolver solver = new CompositeSolver(core, defer, baseSolver, equalitySolver, defer,
                        defer, defer, defer, scopeGraphSolver, defer);
                solver.addAll(constraints);
                solver.iterate();
                equalityResult1 = equalitySolver.finish();
                scopeGraphResult1 = scopeGraphSolver.finish();
                deferred1 = defer.finish();
                unsolved.addAll(scopeGraphResult1.residualConstraints());
                coreResult1 = core.finish();
            }

            final CoreResult coreResult2;
            final AstResult astResult2;
            final EqualityResult equalityResult2;
            final NameResolutionResult nameResolutionResult2;
            final RelationResult relationResult2;
            final SetResult setResult2;
            {
                final SolverCore core = new SolverCore(config, fresh, cancel, progress, coreResult1);
                final ThrowingSolver<IConstraint> deny = new ThrowingSolver<>(core);
                final AstSolver astSolver = new AstSolver(core);
                final EqualitySolver equalitySolver = new EqualitySolver(core, equalityResult1);
                final NameResolutionSolver nameResolutionSolver = new NameResolutionSolver(core,
                        scopeGraphResult1.scopeGraph(), config.getResolutionParams(), Set.Immutable.of());
                final PolySolver polySolver = new PolySolver(core);
                final RelationSolver relationSolver =
                        new RelationSolver(core, config.getRelations(), config.getFunctions());
                final SetSolver setSolver = new SetSolver(core, nameResolutionSolver.nameSets());
                final SymbolicSolver symSolver = new SymbolicSolver(core);
                final CompositeSolver solver = new CompositeSolver(core, astSolver, deny, equalitySolver,
                        nameResolutionSolver, polySolver, relationSolver, setSolver, deny, symSolver);
                solver.addAll(deferred1.residualConstraints());
                solver.iterate();

                coreResult2 = core.finish();

                astResult2 = astSolver.finish();

                equalityResult2 = equalitySolver.finish();
                unsolved.addAll(equalityResult2.residualConstraints());

                nameResolutionResult2 = nameResolutionSolver.finish();
                unsolved.addAll(nameResolutionResult2.residualConstraints());

                relationResult2 = relationSolver.finish();
                unsolved.addAll(relationResult2.residualConstraints());

                setResult2 = setSolver.finish();
                unsolved.addAll(setResult2.residualConstraints());
            }

            return ImmutableSolution.of(config, astResult2.properties(), nameResolutionResult2.scopeGraph(),
                    nameResolutionResult2.nameResolution(), nameResolutionResult2.properties(), Relations.empty(),
                    coreResult2.unifier(), SymbolicConstraints.empty(), coreResult2.messages(), unsolved);
        } catch(RuntimeException ex) {
            throw new SolverException("Internal solver error.", ex);
        }

    }

}