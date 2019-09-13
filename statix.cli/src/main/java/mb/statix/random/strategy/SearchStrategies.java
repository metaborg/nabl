package mb.statix.random.strategy;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.functions.Predicate2;
import org.metaborg.util.optionals.Optionals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.oracle.truffle.api.object.dsl.Nullable;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.FocusedSearchState;
import mb.statix.random.SearchContext;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.scopegraph.Env;
import mb.statix.random.scopegraph.NameResolution;
import mb.statix.random.util.RuleUtil;
import mb.statix.random.util.WeightedDrawSet;
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
import mb.statix.spec.Rule;
import mb.statix.spoofax.StatixTerms;

public final class SearchStrategies {

    public final <I, O> SearchStrategy<I, O> limit(int n, SearchStrategy<I, O> s) {
        return new SearchStrategy<I, O>() {

            @Override public Stream<SearchNode<O>> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
                return s.apply(ctx, input, parent).limit(n);
            }

            @Override public String toString() {
                return "limit(" + n + ", " + s.toString() + ")";
            }

        };
    }

    public final <I1, I2, O> SearchStrategy<I1, O> seq(SearchStrategy<I1, I2> s1, SearchStrategy<I2, O> s2) {
        return new SearchStrategy<I1, O>() {

            @Override public Stream<SearchNode<O>> doApply(SearchContext ctx, I1 i1, SearchNode<?> parent) {
                return s1.apply(ctx, i1, parent).flatMap(sn1 -> {
                    return s2.apply(ctx, sn1.output(), sn1).map(sn2 -> {
                        return new SearchNode<>(sn2.output(), sn2, "(" + sn1.toString() + " . " + sn2.toString() + ")");
                    });
                });
            }

            @Override public String toString() {
                return "(" + s1.toString() + " . " + s2.toString() + ")";
            }

        };
    }

    @SafeVarargs public final <I, O> SearchStrategy<I, O> alt(SearchStrategy<I, O>... ss) {
        final List<SearchStrategy<I, O>> _ss = ImmutableList.copyOf(ss);
        return new SearchStrategy<I, O>() {

            @Override public Stream<SearchNode<O>> doApply(SearchContext ctx, I input, SearchNode<?> parent) {
                final List<Iterator<SearchNode<O>>> _ns = _ss.stream().map(s -> s.apply(ctx, input, parent).iterator())
                        .collect(Collectors.toCollection(ArrayList::new));
                return Streams.stream(new Iterator<SearchNode<O>>() {

                    private void removeEmpty() {
                        final Iterator<Iterator<SearchNode<O>>> it = _ns.iterator();
                        while(it.hasNext()) {
                            if(!it.next().hasNext()) {
                                it.remove();
                            }
                        }
                    }

                    @Override public boolean hasNext() {
                        removeEmpty();
                        return !_ns.isEmpty();
                    }

                    @Override public SearchNode<O> next() {
                        removeEmpty();
                        return _ns.get(ctx.rnd().nextInt(_ns.size())).next();
                    }

                });
            }

            @Override public String toString() {
                return _ss.stream().map(Object::toString).collect(Collectors.joining(" | ", "(", ")"));
            }

        };
    }

    public final SearchStrategy<SearchState, SearchState> infer() {
        return new SearchStrategy<SearchState, SearchState>() {

            @Override public Stream<SearchNode<SearchState>> doApply(SearchContext ctx, SearchState state,
                    SearchNode<?> parent) {
                final SolverResult resultConfig;
                try {
                    resultConfig = Solver.solve(state.state(), Constraints.conjoin(state.constraints()),
                            new NullDebugContext());
                } catch(InterruptedException e) {
                    throw new MetaborgRuntimeException(e);
                }
                if(resultConfig.hasErrors()) {
                    final String msg = Constraints.toString(resultConfig.errors(),
                            new UnifierFormatter(resultConfig.state().unifier(), 3));
                    ctx.addFailed(new SearchNode<>(state, parent, "infer[" + msg + "]"));
                    return Stream.empty();
                }
                final SearchState newState = state.update(resultConfig);
                return Stream.of(new SearchNode<>(newState, parent, "infer"));
            }

            @Override public String toString() {
                return "infer";
            }

        };
    }

    public final <C extends IConstraint> SearchStrategy<SearchState, FocusedSearchState<C>> select(Class<C> cls,
            Predicate1<C> include) {
        return new SearchStrategy<SearchState, FocusedSearchState<C>>() {

            @Override protected Stream<SearchNode<FocusedSearchState<C>>> doApply(SearchContext ctx, SearchState input,
                    SearchNode<?> parent) {
                @SuppressWarnings("unchecked") final Set.Immutable<C> candidates =
                        input.constraints().stream().filter(c -> cls.isInstance(c)).map(c -> (C) c)
                                .filter(include::test).collect(CapsuleCollectors.toSet());
                if(candidates.isEmpty()) {
//                    ctx.addFailed(new SearchNode<>(input, parent, this.toString() + "[no candidates]"));
                    return Stream.empty();
                }
                return WeightedDrawSet.of(candidates).enumerate(ctx.rnd()).map(c -> {
                    final FocusedSearchState<C> output = FocusedSearchState.of(input, c.getKey());
                    return new SearchNode<>(output, parent, "select(" + c.getKey() + ")");
                });
            }

            @Override public String toString() {
                return "select(" + cls.getSimpleName() + ", " + include.toString() + ")";
            }

        };
    }

    @SafeVarargs public final SearchStrategy<SearchState, SearchState> drop(Class<? extends IConstraint>... classes) {
        final ImmutableSet<Class<? extends IConstraint>> _classes = ImmutableSet.copyOf(classes);
        return new SearchStrategy<SearchState, SearchState>() {

            @Override protected Stream<SearchNode<SearchState>> doApply(SearchContext ctx, SearchState input,
                    SearchNode<?> parent) {
                final Set.Immutable<IConstraint> constraints = input.constraints().stream()
                        .filter(c -> !_classes.contains(c.getClass())).collect(CapsuleCollectors.toSet());
                final SearchState output = input.update(input.state(), constraints);
                final String desc =
                        "drop" + _classes.stream().map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
                return Stream.of(new SearchNode<>(output, parent, desc));
            }

            @Override public String toString() {
                return "drop" + _classes.stream().map(Class::getSimpleName).collect(Collectors.joining(", ", "(", ")"));
            }

        };
    }

    public final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand() {
        return expand(ImmutableMap.of());
    }

    public final SearchStrategy<FocusedSearchState<CUser>, SearchState> expand(Map<String, Integer> weights) {
        return new SearchStrategy<FocusedSearchState<CUser>, SearchState>() {

            @Override protected Stream<SearchNode<SearchState>> doApply(SearchContext ctx,
                    FocusedSearchState<CUser> input, SearchNode<?> parent) {
                final CUser predicate = input.focus();
                final Map<Rule, Integer> rules = new HashMap<>();
                for(Rule rule : input.state().spec().rules().get(predicate.name())) {
                    rules.put(rule, weights.getOrDefault(rule.label(), 1));
                }
                return WeightedDrawSet.of(rules).enumerate(ctx.rnd()).map(Map.Entry::getKey).flatMap(rule -> {
                    return Streams.stream(RuleUtil.apply(input.state(), rule, predicate.args(), predicate))
                            .map(result -> {
                                final SearchState output =
                                        input.update(result._1(), input.constraints().__insert(result._2()));
                                final String head = rule.name() + rule.params().stream().map(Object::toString)
                                        .collect(Collectors.joining(", ", "(", ")"));
                                return new SearchNode<>(output, parent, "expand(" + head + ")");
                            });
                });
            }

            @Override public String toString() {
                return "expand" + weights.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue())
                        .collect(Collectors.joining(", ", "(", ")"));
            }

        };
    }

    public final SearchStrategy<FocusedSearchState<CResolveQuery>, SearchState> resolve() {
        return new SearchStrategy<FocusedSearchState<CResolveQuery>, SearchState>() {

            @Override protected Stream<SearchNode<SearchState>> doApply(SearchContext ctx,
                    FocusedSearchState<CResolveQuery> input, SearchNode<?> parent) {
                final State state = input.state();
                final IUnifier unifier = state.unifier();
                final CResolveQuery query = input.focus();

                final Scope scope = Scope.matcher().match(query.scopeTerm(), unifier).orElse(null);
                if(scope == null) {
                    ctx.addFailed(new SearchNode<>(input, parent, "resolve[no scope]"));
                    return Stream.empty();
                }

                final Boolean isAlways;
                try {
                    isAlways = query.min().getDataEquiv().isAlways(state.spec()).orElse(null);
                } catch(InterruptedException e) {
                    throw new MetaborgRuntimeException(e);
                }
                if(isAlways == null) {
                    ctx.addFailed(new SearchNode<>(input, parent, "resolve[cannot decide data equiv]"));
                    return Stream.empty();
                }

                final ICompleteness completeness = new IncrementalCompleteness(state.spec());
                completeness.addAll(input.constraints(), unifier);
                final IsComplete isComplete3 = (s, l, st) -> completeness.isComplete(s, l, st.unifier());
                final Predicate2<Scope, ITerm> isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
                final LabelWF<ITerm> labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
                final LabelOrder<ITerm> labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
                final DataWF<ITerm, CEqual> dataWF =
                        new ConstraintDataWF(isComplete3, state, query.filter().getDataWF(), query);
                //                lengths = length((IListTerm) query.resultTerm(), state.unifier()).map(ImmutableList::of)
                //                        .orElse(ImmutableList.of(0, 1, 2, -1));

                // @formatter:off
                final NameResolution<Scope, ITerm, ITerm, CEqual> nameResolution = new NameResolution<>(
                        state.scopeGraph(), query.relation(),
                        labelWF, labelOrd, isComplete2,
                        dataWF, isAlways, isComplete2);
                // @formatter:on

                final AtomicInteger count = new AtomicInteger(1);
                try {
                    nameResolution.resolve(scope, () -> {
                        count.incrementAndGet();
                        return false;
                    });
                } catch(ResolutionException e) {
                    ctx.addFailed(new SearchNode<>(input, parent, "resolve[counting error:" + e.getMessage() + "]"));
                    return Stream.empty();
                } catch(InterruptedException e) {
                    throw new MetaborgRuntimeException(e);
                }

                final List<Integer> indices =
                        IntStream.range(0, count.get()).boxed().collect(Collectors.toCollection(ArrayList::new));
                Collections.shuffle(indices, ctx.rnd());

                return indices.stream().flatMap(idx -> {
                    final AtomicInteger select = new AtomicInteger(idx);
                    final Env<Scope, ITerm, ITerm, CEqual> env;
                    try {
                        env = nameResolution.resolve(scope, () -> select.getAndDecrement() == 0);
                    } catch(ResolutionException e) {
                        ctx.addFailed(
                                new SearchNode<>(input, parent, "resolve[resolution error:" + e.getMessage() + "]"));
                        return Stream.empty();
                    } catch(InterruptedException e) {
                        throw new MetaborgRuntimeException(e);
                    }

                    return WeightedDrawSet.of(env.matches).enumerate(ctx.rnd()).map(entry -> {
                        final Env.Builder<Scope, ITerm, ITerm, CEqual> subEnv = Env.builder();
                        subEnv.match(entry.getKey());
                        entry.getValue().forEach(subEnv::reject);
                        env.rejects.forEach(subEnv::reject);
                        return subEnv.build();
                    }).map(subEnv -> {
                        final List<ITerm> pathTerms = subEnv.matches.stream().map(m -> StatixTerms.explicate(m.path))
                                .collect(ImmutableList.toImmutableList());
                        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
                        constraints.add(new CEqual(B.newList(pathTerms), query.resultTerm(), query));
                        subEnv.matches.stream().flatMap(m -> Optionals.stream(m.condition))
                                .forEach(condition -> constraints.add(condition));
                        subEnv.rejects.stream().flatMap(m -> Optionals.stream(m.condition))
                                .forEach(condition -> constraints.add(new CInequal(condition.term1(), condition.term2(),
                                        condition.cause().orElse(null))));
                        constraints.addAll(input.constraints());
                        final SearchState newState = input.update(input.state(), constraints.build());
                        return new SearchNode<>(newState, parent, "resolve[" + idx + "/" + count.get() + "]");
                    });
                });
            }

            @Override public String toString() {
                return "resolve";
            }

            class ConstraintDataWF implements DataWF<ITerm, CEqual> {
                private final IsComplete isComplete3;
                private final State state;
                private final Rule dataWf;
                private final IConstraint cause;

                private ConstraintDataWF(IsComplete isComplete3, State state, Rule dataWf,
                        @Nullable IConstraint cause) {
                    this.isComplete3 = isComplete3;
                    this.state = state;
                    this.dataWf = dataWf;
                    this.cause = cause;
                }

                @Override public Optional<Optional<CEqual>> wf(ITerm datum)
                        throws ResolutionException, InterruptedException {
                    // remove all previously created variables/scopes to make them rigid/closed
                    State state = this.state.clearVarsAndScopes();

                    // apply rule
                    final Optional<Tuple2<State, IConstraint>> stateAndConstraint =
                            RuleUtil.apply(state, dataWf, ImmutableList.of(datum), null);
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
                    final CEqual eq = new CEqual(B.newTuple(leftTerms), B.newTuple(rightTerms), cause);
                    return Optional.of(Optional.of(eq));
                }

            }

        };
    }

}