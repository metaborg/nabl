package mb.statix.taico.incremental.strategy;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.metaborg.util.functions.Function1;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.statix.solver.IConstraint;
import mb.statix.solver.ISolverResult;
import mb.statix.solver.log.IDebugContext;
import mb.statix.taico.incremental.IChangeSet;
import mb.statix.taico.module.ModuleManager;
import mb.statix.taico.solver.IMState;

public interface IncrementalStrategy {
    /**
     * Reanalyzes using this incremental strategy.
     * 
     * @param topModule
     *      the top module
     * @param manager
     *      the module manager to set the modules to be solved in (TODO)
     * @param changeSet
     *      the change set
     */
    void setupReanalysis(ModuleManager manager, IChangeSet changeSet);

    /**
     * Reanalyzes modules in an incremental fashion depending on the strategy.
     * 
     * <p>This method should be called only after the {@link #setupReanalysis} method has been called.
     * 
     * @param changeSet
     *      the change set
     * @param baseState
     *      the state to start from
     * @param moduleConstraints
     *      a map from module name to constraints to solve
     * @param debug
     *      the debug context
     * 
     * @return
     *      a map of results, based on the changeset
     * 
     * @throws InterruptedException
     *      If solving is interrupted.
     */
    Map<String, ISolverResult> reanalyze(IChangeSet changeSet, IMState baseState, Map<String, Set<IConstraint>> moduleConstraints, IDebugContext debug) throws InterruptedException;
    
    /**
     * @return
     *      a matcher for incremental strategies
     */
    public static IMatcher<IncrementalStrategy> matcher() {
        Function<String, IncrementalStrategy> f = s -> {
            switch (s) {
                case "default":
                case "baseline":
                    return new BaselineIncrementalStrategy();
                //TODO Add more strategies here
                default:
                    return null;
            }
        };
        Function1<ITerm, Optional<IncrementalStrategy>> empty = i -> Optional.empty();
        return (term, unifier) -> unifier.findTerm(term).match(Terms.<Optional<IncrementalStrategy>>cases(empty, empty,
                string -> Optional.ofNullable(f.apply(string.getValue())), empty, empty, empty));
    }
}
