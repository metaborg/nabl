package mb.statix.random.node;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.scopegraph.Env;
import mb.statix.random.scopegraph.Match;
import mb.statix.random.scopegraph.NameResolution;
import mb.statix.random.util.ElementSelectorSet;
import mb.statix.random.util.ElementSelectorSet.Entry;
import mb.statix.scopegraph.reference.LabelOrder;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.completeness.IncrementalCompleteness;
import mb.statix.solver.completeness.IsComplete;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.query.RegExpLabelWF;
import mb.statix.solver.query.RelationLabelOrder;
import mb.statix.spoofax.StatixTerms;

public class ResolveQuery extends SearchNode<Tuple2<SearchState, Tuple3<CResolveQuery, Scope, Boolean>>, SearchState> {

    public ResolveQuery(Random rnd) {
        super(rnd);
    }

    private CResolveQuery query;
    private Scope scope;
    private NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution;
    private List<Integer> lengths;

    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicInteger step = new AtomicInteger();
    private final Deque<Env<Scope, ITerm, ITerm, CEqual>> envs = new LinkedList<>();

    @Override protected void doInit() {
        this.query = input._2()._1();
        this.scope = input._2()._2();

        // set-up parameters
        final State state = input._1().state();
        boolean dataEq = input._2()._3();
        final ICompleteness completeness = new IncrementalCompleteness(state.spec());
        completeness.addAll(input._1().constraints(), state.unifier());
        final IsComplete isComplete3 = (s, l, st) -> completeness.isComplete(s, l, st.unifier());
        final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
        final DataWF<ITerm, CEqual> dataWF = new ConstraintDataWF(isComplete3, state);
        lengths = length((IListTerm) query.resultTerm(), state.unifier()).map(ImmutableList::of)
                .orElse(ImmutableList.of(0, 1, 2, -1));

        // @formatter:off
        nameResolution = new NameResolution<>(
                state.scopeGraph(), query.relation(),
                labelWF, labelOrd, isComplete2,
                dataWF, dataEq, isComplete2);
        // @formatter:on

        done.set(false);
        step.set(0);
        envs.clear();
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException, InterruptedException {
        if(envs.isEmpty()) {
            if(done.get()) {
                return Optional.empty();
            }
            // resolve
            final AtomicInteger select = new AtomicInteger(step.incrementAndGet());
            final Env<Scope, ITerm, ITerm, CEqual> env;
            try {
                env = nameResolution.resolve(scope, () -> select.decrementAndGet() == 0);
            } catch(ResolutionException e) {
                return Optional.empty();
            }
            if(select.get() > 0) {
                done.set(true);
            }
            envs.addAll(selectAll(env));
            return doNext();
        }

        // create output
        final Env<Scope, ITerm, ITerm, CEqual> env = envs.pop();
        final List<ITerm> pathTerms =
                env.matches.stream().map(m -> StatixTerms.explicate(m.path)).collect(ImmutableList.toImmutableList());
        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        constraints.add(new CEqual(B.newList(pathTerms), query.resultTerm(), query));
        env.matches.stream().flatMap(m -> Optionals.stream(m.condition))
                .forEach(condition -> constraints.add(condition));
        env.rejects.stream().flatMap(m -> Optionals.stream(m.condition)).forEach(condition -> constraints
                .add(new CInequal(condition.term1(), condition.term2(), condition.cause().orElse(null))));
        constraints.addAll(input._1().constraints());
        final SearchState newState = input._1().update(input._1().state(), constraints.build());
        return Optional.of(newState);
    }

    private Collection<Env<Scope, ITerm, ITerm, CEqual>> selectAll(Env<Scope, ITerm, ITerm, CEqual> env) {
        final ImmutableList.Builder<Env<Scope, ITerm, ITerm, CEqual>> envs = ImmutableList.builder();
        for(Entry<Match<Scope, ITerm, ITerm, CEqual>> entry : new ElementSelectorSet<>(env.matches)) {
            final Env.Builder<Scope, ITerm, ITerm, CEqual> subEnv = Env.builder();
            subEnv.match(entry.getFocus());
            entry.getOthers().forEach(subEnv::reject);
            env.rejects.forEach(subEnv::reject);
            envs.add(subEnv.build());
        }
        return envs.build();
    }

    @Override public String toString() {
        return "resolve-query";
    }

    private class ConstraintDataWF implements DataWF<ITerm, CEqual> {
        private final IsComplete isComplete3;
        private final State state;

        private ConstraintDataWF(IsComplete isComplete3, State state) {
            this.isComplete3 = isComplete3;
            this.state = state;
        }

        @Override public Optional<Optional<CEqual>> wf(ITerm datum) throws ResolutionException, InterruptedException {
            // remove all previously created variables/scopes to make them rigid/closed
            State state = this.state.clearVarsAndScopes();

            // apply rule
            final Optional<Tuple2<State, IConstraint>> stateAndConstraint =
                    apply(state, query.filter().getDataWF(), ImmutableList.of(datum), query);
            if(!stateAndConstraint.isPresent()) {
                return Optional.empty();
            }
            state = stateAndConstraint.get()._1();
            final IConstraint constraint = stateAndConstraint.get()._2();

            // solve rule constraint
            final SolverResult result = Solver.solve(state, constraint, isComplete3, new NullDebugContext());
            if(result.hasErrors()) {
                return Optional.empty();
            }
            if(!result.delays().keySet().stream().allMatch(c -> c instanceof CEqual)) {
                return Optional.empty();
            }
            if(result.delays().isEmpty()) {
                return Optional.of(Optional.empty());
            }

            final List<ITerm> leftTerms = Lists.newArrayList();
            final List<ITerm> rightTerms = Lists.newArrayList();
            result.delays().keySet().stream().map(c -> (CEqual) c).forEach(eq -> {
                leftTerms.add(eq.term1());
                rightTerms.add(eq.term2());
            });
            final CEqual eq = new CEqual(B.newTuple(leftTerms), B.newTuple(rightTerms), query);
            return Optional.of(Optional.of(eq));
        }

    }

    private static Optional<Integer> length(IListTerm list, IUnifier unifier) {
        // @formatter:off
        return M.<Integer>casesFix(m -> Arrays.asList(
            M.cons(cons -> cons).flatMap(cons -> m.match(cons.getTail(), unifier).map(l -> 1 + l)),
            M.nil(nil -> 0)
        )).match(list, unifier);
        // @formatter:on
    }

}