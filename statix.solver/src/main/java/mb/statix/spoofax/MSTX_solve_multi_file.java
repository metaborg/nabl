package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TOverrides.*;

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
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.spec.Spec;
import mb.statix.taico.incremental.MChange;
import mb.statix.taico.incremental.changeset.IChangeSet;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.scopegraph.diff.Diff;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.Context;
import mb.statix.taico.solver.concurrent.ConcurrentSolverCoordinator;
import mb.statix.taico.solver.coordinator.ISolverCoordinator;
import mb.statix.taico.solver.coordinator.SolverCoordinator;
import mb.statix.taico.solver.state.IMState;
import mb.statix.taico.util.TDebug;
import mb.statix.taico.util.TOverrides;
import mb.statix.taico.util.TTimings;

public class MSTX_solve_multi_file extends StatixPrimitive {

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
        
        TTimings.startPhase("init");
        final IncrementalStrategy strategy = IncrementalStrategy.matcher().match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Invalid incremental strategy: " + terms.get(0)));
        
        final MSolverResult initial = M.blobValue(MSolverResult.class).match(terms.get(1))
                .orElseThrow(() -> new InterpreterException("Expected solver result, but was " + terms.get(1)))
                .reset();
        
        final Spec spec = initial.context().getSpec();
        Objects.requireNonNull(spec, "The spec should never be null");

        final IDebugContext debug = getDebugContext(terms.get(2));

        final IMatcher<Tuple2<MChange, IConstraint>> constraintMatcher = M.tuple2(
                MChange.matcher(),
                StatixTerms.constraint(),
                (t, mc, c) -> ImmutableTuple2.of(mc, c));
        
        TTimings.startPhase("constraint matching");
        final List<Tuple2<MChange, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints, but was " + term));
        TTimings.endPhase("constraint matching");
        
        
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
                    System.err.println("Removed: " + change.getModule());
                    removed.add(change.getModule());
                    continue;
                case CHANGED:
                    System.err.println("Changed: " + change.getModule());
                    changed.add(change.getModule());
                    break;
                case ADDED:
                    System.err.println("Added: " + change.getModule());
                    added.add(change.getModule());
                    break;
                default:
                    System.err.println("Unchanged: " + change.getModule());
                    break;
            }
            order.put(change.getModule(), i++);
            modules.put(change.getModule(), tuple._2());
        }
        
        Context oldContext = initial.context();
        IChangeSet changeSet = strategy.createChangeSet(oldContext, added, changed, removed);
        TTimings.endPhase("changeset");
        
        TTimings.startPhase("incremental context");
        Context newContext = Context.incrementalContext(strategy, oldContext, initial.state(), changeSet, modules, spec);
        TTimings.endPhase("incremental context");
        
        ISolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator(Executors.newWorkStealingPool(THREADS)) : new SolverCoordinator();
        newContext.setCoordinator(coordinator); //Sets the coordinator on the context and the context on the coordinator
        
        TTimings.endPhase("init");
        TTimings.startPhase("solving");
        
        //Do the actual analysis
        Map<String, ISolverResult> results;
        try {
            IMState rootState = initial.state().owner().getCurrentState();
            results = coordinator.solve(strategy, changeSet, rootState, modules, debug);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TTimings.endPhase("solving");
        
        if (OUTPUT_DIFF) Diff.diff(initial.state().owner().getId(), newContext, oldContext, true).print(System.out);
        
        TTimings.startPhase("commit changes");
        newContext.commitChanges();
        TTimings.endPhase("commit changes");

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
        return tbr;
    }
}