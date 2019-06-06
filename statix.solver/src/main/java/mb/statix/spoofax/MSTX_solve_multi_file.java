package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.taico.util.TOverrides.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import mb.statix.taico.incremental.changeset.IChangeSet2;
import mb.statix.taico.incremental.strategy.IncrementalStrategy;
import mb.statix.taico.solver.ASolverCoordinator;
import mb.statix.taico.solver.MSolverResult;
import mb.statix.taico.solver.SolverContext;
import mb.statix.taico.solver.SolverCoordinator;
import mb.statix.taico.solver.concurrent.ConcurrentSolverCoordinator;

public class MSTX_solve_multi_file extends StatixPrimitive {

    @Inject public MSTX_solve_multi_file() {
        super(MSTX_solve_multi_file.class.getSimpleName(), 3);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if (CLEAN) throw new UnsupportedOperationException("Throwing error to clean build!");
        
        final IncrementalStrategy strategy = IncrementalStrategy.matcher().match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Invalid incremental strategy: " + terms.get(0)));
        
        final MSolverResult initial = M.blobValue(MSolverResult.class).match(terms.get(1))
                .orElseThrow(() -> new InterpreterException("Expected solver result, but was " + terms.get(1)))
                .reset();
        final Spec spec = initial.state().spec();

        final IDebugContext debug = getDebugContext(terms.get(2));

        final IMatcher<Tuple2<MChange, IConstraint>> constraintMatcher = M.tuple2(
                MChange.matcher(),
                StatixTerms.constraint(),
                (t, mc, c) -> ImmutableTuple2.of(mc, c));
        
        final List<Tuple2<MChange, IConstraint>> constraints = M.listElems(constraintMatcher).match(term)
                .orElseThrow(() -> new InterpreterException("Expected list of constraints, but was " + term));
        
        //We want the "initial" state, but rather we want the previous module manager used for the initial state.
        //TODO IMPORTANT Check if the changes to the module manager applied further on are applied on the project
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
        SolverContext oldContext = initial.context();
        IChangeSet2 changeSet = strategy.createChangeSet(oldContext, added, changed, removed);
        
        SolverContext newContext = SolverContext.incrementalContext(strategy, oldContext, initial.state(), changeSet, modules, spec);
//        newContext.setState(initial.state().getOwner(), initial.state());
        
        ASolverCoordinator coordinator = CONCURRENT ? new ConcurrentSolverCoordinator(Executors.newWorkStealingPool(THREADS)) : new SolverCoordinator();
        newContext.setCoordinator(coordinator); //Sets the coordinator on the context and the context on the coordinator
        
        //TODO IMPORTANT Solver Context
        
        //Do the actual analysis
        Map<String, ISolverResult> results;
        try {
            //TODO Add the context as an argument to the coordinator?
            results = coordinator.solve(strategy, changeSet, initial.state(), modules, debug);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        newContext.commitChanges();
        //TODO This is wrong!
        System.err.println("Modules in the context post solve: " + newContext.getModules());

//        List<ITerm> strategoResults = results.entrySet().stream()
//                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
//                .map(e -> {
//                    String name = e.getKey();
//                    boolean full = added.contains(name) || changed.contains(name);
//                    return B.newTuple(B.newString(name), B.newAppl(full ? "F" : "U"), B.newBlob(e.getValue()));
//                })
//                .collect(Collectors.toList());
        
        //Return a tuple of 2 lists, one for added + changed (dirty) results, one for cached (clirty) results.
        List<ITerm> fullResults = results.entrySet().stream()
                .filter(e -> order.containsKey(e.getKey())) //TODO Should not be needed
                .filter(e -> added.contains(e.getKey()) || changed.contains(e.getKey()))
                .sorted((a, b) -> Integer.compare(order.get(a.getKey()), order.get(b.getKey())))
                .map(e -> B.newTuple(B.newString(e.getKey()), B.newBlob(e.getValue())))
                .collect(Collectors.toList());
        
        List<ITerm> updateResults = results.entrySet().stream()
                .filter(e -> order.containsKey(e.getKey())) //TODO Should not be needed
                .filter(e -> !added.contains(e.getKey()) && !changed.contains(e.getKey()))
                .sorted((a, b) -> Integer.compare(order.get(a.getKey()), order.get(b.getKey())))
                .map(e -> B.newTuple(B.newString(e.getKey()), B.newBlob(e.getValue())))
                .collect(Collectors.toList());
        
        // ([(resource, SolverResult)], [(resource, SolverResult)])
        return Optional.of(B.newTuple(B.newList(fullResults), B.newList(updateResults)));
    }

}