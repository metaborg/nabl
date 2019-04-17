package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.statix.taico.module.IModule;
import mb.statix.taico.module.Module;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.ISolverCoordinator;
import mb.statix.taico.solver.MState;
import mb.statix.taico.solver.SolverCoordinator;
import mb.statix.taico.solver.concurrent.ConcurrentSolverCoordinator;

public class MSTX_solve_constraint extends StatixPrimitive {
    private static final boolean DEBUG = true;
    private static final boolean CONCURRENT = true;
    public static final boolean QUERY_DEBUG = false;

    @Inject public MSTX_solve_constraint() {
        super(MSTX_solve_constraint.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));
        reportOverlappingRules(spec);

        final IDebugContext debug = getDebugContext(DEBUG ? B.newString("debug") : terms.get(1));

        final Tuple2<List<ITermVar>, Set<IConstraint>> vars_constraint = M
                .tuple2(M.listElems(StatixTerms.varTerm()), StatixTerms.constraints(spec.labels()),
                        (t, vs, c) -> ImmutableTuple2.of(vs, c))
                .match(term).orElseThrow(() -> new InterpreterException("Expected constraint."));

        final ITerm resultTerm = solveConstraint(spec, vars_constraint._1(), vars_constraint._2(), debug);
        return Optional.of(resultTerm);
    }

    private ITerm solveConstraint(Spec spec, List<ITermVar> topLevelVars, Set<IConstraint> constraints,
            IDebugContext debug) {
        //TODO TAICO Determine ID from somewhere for this module
        final ModuleManager manager = new ModuleManager();
        final IModule module = new Module(manager, "G", spec);
        final ISolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator() : new SolverCoordinator();
        final MState state = new MState(manager, coordinator, module, spec);
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        for(ITermVar var : topLevelVars) {
            final ITermVar nvar = state.freshVar(var.getName());
            subst.put(var, nvar);
            subst.put(nvar, var);
        }
        final ISubstitution.Immutable isubst = subst.freeze();
        
        constraints = constraints.stream().map(c -> c.apply(isubst)).collect(Collectors.toSet());
        
        final ISolverResult resultConfig;
        try {
            resultConfig = coordinator.solve(state, constraints, debug);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        final IUnifier.Immutable unifier = state.unifier();

        final List<ITerm> errorList = Lists.newArrayList();
        if(resultConfig.hasErrors()) {
            resultConfig.errors().stream().map(c -> makeMessage("Failed", c, unifier)).forEach(errorList::add);
        }

        final Collection<IConstraint> unsolved = resultConfig.delays().keySet();
        if(!unsolved.isEmpty()) {
            unsolved.stream().map(c -> makeMessage("Unsolved", c, unifier)).forEach(errorList::add);
        }

        final ITerm substTerm =
                StatixTerms.explicateMapEntries(toplevelSubstitution(topLevelVars, isubst, state.unifier()).entrySet());
        final ITerm solverTerm = B.newBlob(resultConfig.withDelays(ImmutableMap.of()).withErrors(ImmutableSet.of()));
        final ITerm solveResultTerm = B.newAppl("Solution", substTerm, solverTerm);
        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(solveResultTerm, errors, warnings, notes);

        return resultTerm;
    }
}
