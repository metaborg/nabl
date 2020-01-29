package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.modular.util.TOverrides.CONCURRENT;
import static mb.statix.modular.util.TOverrides.OUTPUT_DIFF;
import static mb.statix.modular.util.TOverrides.THREADS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.modular.incremental.MChange;
import mb.statix.modular.incremental.changeset.IChangeSet;
import mb.statix.modular.incremental.manager.IncrementalManager;
import mb.statix.modular.incremental.strategy.IncrementalStrategy;
import mb.statix.modular.solver.Context;
import mb.statix.modular.solver.MSolverResult;
import mb.statix.modular.solver.concurrent.ConcurrentSolverCoordinator;
import mb.statix.modular.solver.coordinator.ISolverCoordinator;
import mb.statix.modular.solver.coordinator.SolverCoordinator;
import mb.statix.modular.solver.state.IMState;
import mb.statix.modular.util.TDebug;
import mb.statix.modular.util.TOverrides;
import mb.statix.modular.util.TTimings;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;

public class MSTX_solve_multi_file extends StatixPrimitive {
    public static long diffTime, depTime, solveTime, totalTime;
    public static int phaseCount, remoduleCount, moduleCount, stateCount;
    
    @Inject public MSTX_solve_multi_file() {
        super(MSTX_solve_multi_file.class.getSimpleName(), 3);
    }
    
    @Override
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startPhase("MSTX_solve_multi_file", "Strategy=" + terms.get(0), "Settings: " + TOverrides.print(), "Debug: " + TDebug.print());
        
        try {
            return super._call(env, term, terms);
        } finally {
            TTimings.endPhase("MSTX_solve_multi_file");
        }
    }
    
    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        long mStart = System.currentTimeMillis();
        TTimings.startPhase("init");
        
        final IncrementalStrategy strategy = getStrategy(terms);
        final MSolverResult initial = getPreviousResult(terms);
        
        final Spec spec = initial.context().getSpec();
        Objects.requireNonNull(spec, "The spec should never be null");

        final IDebugContext debug = getDebugContext(terms.get(2));
        terms = null; //Memory

        final IMatcher<Tuple2<MChange, IConstraint>> constraintMatcher = M.tuple2(
                MChange.matcher(),
                StatixTerms.constraint(),
                (t, mc, c) -> ImmutableTuple2.of(mc, c));
        
        List<Tuple2<MChange, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints, but was " + term));
        
        TTimings.startPhase("changeset");
        //Temporarily set the context to the previous context for the changeset
        Context.setContext(initial.context());
        Set<String> removed = new HashSet<>();
        Set<String> changed = new HashSet<>();
        Set<String> added = new HashSet<>();
        //TODO is the order still necessary?
        Map<String, Integer> order = new HashMap<>();
        Map<String, IConstraint> modules = new HashMap<>();
        int i = 0;
        for (Tuple2<MChange, IConstraint> tuple : constraints) {
            MChange change = tuple._1();
            switch (change.getChangeType()) {
                case REMOVED:
                    if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Removed: " + change.getModule());
                    removed.add(change.getModule());
                    continue;
                case CHANGED:
                    if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Changed: " + change.getModule());
                    changed.add(change.getModule());
                    break;
                case ADDED:
                    if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Added: " + change.getModule());
                    added.add(change.getModule());
                    break;
                default:
                    if (TDebug.CHANGESET) TDebug.DEV_OUT.info("Unchanged: " + change.getModule());
                    break;
            }
            order.put(change.getModule(), i++);
            modules.put(change.getModule(), tuple._2());
        }
        //Memory consideration
        constraints = null;
        
        Context oldContext = initial.context();
        IChangeSet changeSet = strategy.createChangeSet(oldContext, added, changed, removed);
        TTimings.endPhase("changeset");
        
        TTimings.startPhase("incremental context");
        Context newContext = Context.incrementalContext(strategy, oldContext, initial.state(), changeSet, modules, spec);
        TTimings.endPhase("incremental context");
        
        ISolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator(Executors.newWorkStealingPool(THREADS)) : new SolverCoordinator();
        newContext.setCoordinator(coordinator); //Sets the coordinator on the context and the context on the coordinator
        
        TTimings.endPhase("init");
        long solveStart = System.currentTimeMillis();
        TTimings.startPhase("solving", solveStart);
        
        //Do the actual analysis
        Map<String, ISolverResult> results;
        try {
            IMState rootState = initial.state().owner().getCurrentState();
            results = coordinator.solve(strategy, changeSet, rootState, modules, debug);
            modules = null; //Clear memory
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long solveEnd = System.currentTimeMillis();
        TTimings.endPhase("solving", solveEnd);
        solveTime = (solveEnd - solveStart);
        
        if (OUTPUT_DIFF) TDebug.outputDiff(oldContext, newContext, "");
        
        //Explicitly assign null to allow for garbage collection
        oldContext = null;
        
        //For the CLI
        IncrementalManager im = newContext.getIncrementalManager();
        diffTime = im.getTotalDiffTime();
        depTime = im.getTotalDependencyTime();
        phaseCount = im.getPhaseCount();
        remoduleCount = im.getReanalyzedModuleCount();
        moduleCount = newContext.getModules().size();
        stateCount = newContext.getStates().size();
        newContext.commitChanges();

        //Return a tuple of 2 lists, one for added + changed (dirty) results, one for cached (unsure) results.
        TTimings.startPhase("collect results");
        List<ITerm> fullResults = results.entrySet().stream()
                .filter(e -> order.containsKey(e.getKey())) //TODO Should not be needed
                .filter(e -> added.contains(e.getKey()) || changed.contains(e.getKey()))
                .sorted((a, b) -> Integer.compare(order.get(a.getKey()), order.get(b.getKey())))
                .map(e -> B.newTuple(B.newString(e.getKey()), B.newBlob(e.getValue())))
                .collect(Collectors.toList());
        
        List<ITerm> updateResults = results.entrySet().stream()
                .filter(e -> !added.contains(e.getKey()) && !changed.contains(e.getKey()))
                .sorted((a, b) -> Integer.compare(order.get(a.getKey()), order.get(b.getKey())))
                .map(e -> B.newTuple(B.newString(e.getKey()), B.newBlob(e.getValue())))
                .collect(Collectors.toList());
        
        // ([(resource, SolverResult)], [(resource, SolverResult)])
        Optional<ITerm> tbr = Optional.of(B.newTuple(B.newList(fullResults), B.newList(updateResults)));
        TTimings.endPhase("collect results");
        totalTime = System.currentTimeMillis() - mStart;
        return tbr;
    }

    private MSolverResult getPreviousResult(List<ITerm> terms) throws InterpreterException {
        final ITerm solverResult = terms.get(1);
        return M.blobValue(MSolverResult.class).match(solverResult)
                .orElseThrow(() -> new InterpreterException("Expected solver result, but was " + solverResult))
                .reset();
    }

    private IncrementalStrategy getStrategy(List<ITerm> terms) throws InterpreterException {
        final ITerm strategyTerm = terms.get(0);
        return IncrementalStrategy.matcher().match(strategyTerm)
                .orElseThrow(() -> new InterpreterException("Invalid incremental strategy: " + strategyTerm));
    }
}