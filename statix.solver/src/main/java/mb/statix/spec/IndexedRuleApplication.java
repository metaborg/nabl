package mb.statix.spec;

//import static mb.nabl2.terms.build.TermBuild.B;
//import static mb.nabl2.terms.matching.TermPattern.P;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.SortedSet;
//import java.util.TreeSet;
//
//import javax.annotation.Nullable;
//
//import org.metaborg.util.log.ILogger;
//import org.metaborg.util.log.LoggerUtils;
//import org.metaborg.util.task.NullCancel;
//import org.metaborg.util.task.NullProgress;
//
//import io.usethesource.capsule.Set.Immutable;
//import mb.nabl2.terms.ITerm;
//import mb.nabl2.terms.ITermVar;
//import mb.nabl2.terms.matching.Pattern;
//import mb.nabl2.terms.substitution.FreshVars;
//import mb.nabl2.terms.substitution.IRenaming;
//import mb.nabl2.terms.substitution.ISubstitution;
//import mb.nabl2.terms.substitution.PersistentSubstitution;
//import mb.nabl2.terms.substitution.Renaming;
//import mb.nabl2.terms.unification.u.IUnifier;
//import mb.nabl2.terms.unification.ud.IUniDisunifier;
//import mb.statix.solver.Delay;
//import mb.statix.solver.IConstraint;
//import mb.statix.solver.IState;
//import mb.statix.solver.log.NullDebugContext;
//import mb.statix.solver.persistent.Solver;
//import mb.statix.solver.persistent.SolverResult;
//import mb.statix.solver.persistent.State;
//
//public class IndexedRuleApplication {
//
//    private static final ILogger logger = LoggerUtils.logger(IndexedRuleApplication.class);
//
//    /*
//     * In the end we want something like equality: on the index key.
//     * We want to have this two ways:
//     * 1. Given the parameters to the rule, what is the key?
//     * 2. Given an instantiation of the free variables, what is the key?
//     * What if it is not a straight equivalence?
//     * 1. Leftover constraints indicate that the context may rule out some initial matches.
//     * 2. Leftover constraints indicate that the context is insufficiently instantiated? Delay!
//     */
//
//
//    private final List<Pattern> matchers;
//    private final ITerm index;
//    private final Spec spec;
//    private final @Nullable IConstraint constraint;
//
//    private IndexedRuleApplication(List<Pattern> matchers, ITerm index, Spec spec, IConstraint constraint) {
//        this.matchers = matchers;
//        this.constraint = constraint;
//        this.spec = spec;
//        this.index = index;
//    }
//
//    public Optional<ITerm> lookupIndex(IUnifier.Immutable unifier) throws Delay, InterruptedException {
//        return Optional.empty();
//
//    }
//
//    public Optional<ITerm> argumentIndex(List<ITerm> args, IUnifier.Immutable unifier)
//            throws Delay, InterruptedException {
//        //        ISubstitution.Immutable subst;
//        //        if((subst = P.match(matchers, args, unifier).orElse(null)) == null) {
//        return Optional.empty();
//        //        }
//        //        if(constraint != null) {
//        //            final List<IConstraint> eqs = subst.entrySet().stream().map(e -> new CEqual(e.getKey(), e.getValue()))
//        //                    .collect(Collectors.toList());
//        //            Solver.solve(spec, continuation.state(), eqs, continuation.delays(), continuation.completeness(),
//        //                    new NullDebugContext(), new NullProgress(), new NullCancel());
//        //        }
//        //
//        //        final ITerm index = subst.apply(this.index);
//        //        return Optional.of(index);
//
//    }
//
//    public static Optional<IndexedRuleApplication> of(Spec spec, Rule _rule) throws InterruptedException {
//        logger.info("_rule = {}", _rule);
//        final Set.Immutable<ITermVar> freeVars = _rule.freeVars();
//        final FreshVars fresh = new FreshVars();
//
//        // prepare rule and solver state
//        // argument vars should be unifiable, but free vars probably not?
//        final IState.Transient _state = State.of(spec).melt();
//        final Renaming.Builder _renaming = Renaming.builder();
//        for(ITermVar freeVar : freeVars) {
//            _renaming.put(freeVar, _state.freshVar(freeVar));
//        }
//        _state.subState(); // previous variables must be considered rigid
//        final List<ITermVar> args = new ArrayList<>();
//        for(Pattern param : _rule.params()) {
//            args.add(_state.freshVar(B.newVar("", "arg")));
//        }
//        final IState.Immutable state = _state.freeze();
//        final IRenaming renaming = _renaming.build();
//        final Rule rule = _rule.apply(renaming);
//        logger.info("rule = {}", rule);
//
//        final ApplyResult applyResult;
//        if((applyResult = RuleUtil.apply(state, rule, args, null, ApplyMode.RELAXED).orElse(null)) == null) {
//            return Optional.empty();
//        }
//        final SolverResult solveResult = Solver.solve(spec, applyResult.state(), applyResult.body(),
//                new NullDebugContext(), new NullCancel(), new NullProgress());
//
//        final IUniDisunifier.Immutable unifier = solveResult.state().unifier();
//        logger.info("unifier = {}", unifier);
//
//        if(solveResult.hasErrors()) {
//            return Optional.empty();
//        }
//
//        final SortedSet<ITermVar> indexVars = new TreeSet<>();
//        final List<Pattern> matchers = new ArrayList<>();
//        final ISubstitution.Transient _subst = PersistentSubstitution.Transient.of();
//        for(int i = 0; i < args.size(); i++) {
//            final ITermVar arg = args.get(i);
//            final ITerm term = unifier.findRecursive(arg);
//            // FIXME renaming is symmetric, should we only have new freeVars here?
//            final Pattern matcher = P.fromTerm(term, v -> !renaming.valueSet().contains(v));
//            indexVars.addAll(term.getVars().__retainAll(renaming.valueSet()));
//            matchers.add(matcher);
//            _subst.put(arg, term);
//        }
//        logger.info("matchers = {}", matchers);
//        // FIXME We lost the original free vars!
//        final ISubstitution.Immutable subst = _subst.freeze();
//        final ITerm index = B.newTuple(indexVars);
//        logger.info("index = {}", index);
//
//        final IConstraint constraint;
//        if(solveResult.delays().isEmpty()) {
//            constraint = null;
//        } else {
//            // FIXME Internal vars should be properly scopes!
//            constraint = solveResult.delayed().apply(subst);
//            logger.info("constraint = {}", constraint);
//        }
//        final IndexedRuleApplication ira = new IndexedRuleApplication(matchers, index, spec, constraint);
//        return Optional.of(ira);
//    }
//
//}