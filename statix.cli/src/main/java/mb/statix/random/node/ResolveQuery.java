package mb.statix.random.node;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Predicate2;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.Tuple3;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CResolveQuery;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.scopegraph.DataWF;
import mb.statix.random.scopegraph.Env;
import mb.statix.random.scopegraph.NameResolution;
import mb.statix.scopegraph.IScopeGraph;
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

    private final AtomicBoolean fired = new AtomicBoolean();

    private CResolveQuery query;
    private Scope scope;
    private boolean dataEq;
    private IsComplete isComplete;
    private Predicate2<Scope, ITerm> isComplete2;
    private IScopeGraph<Scope, ITerm, ITerm> scopeGraph;
    private LabelWF<ITerm> labelWF;
    private LabelOrder<ITerm> labelOrd;

    @Override protected void doInit() {
        fired.set(false);
        final State state = input._1().state();
        this.query = input._2()._1();
        this.scope = input._2()._2();
        this.dataEq = input._2()._3();
        final ICompleteness completeness = new IncrementalCompleteness(state.spec());
        completeness.addAll(input._1().constraints(), state.unifier());
        this.isComplete = (s, l, st) -> completeness.isComplete(s, l, st.unifier());
        this.isComplete2 = (s, l) -> completeness.isComplete(s, l, state.unifier());
        this.scopeGraph = state.scopeGraph();
        this.labelWF = RegExpLabelWF.of(query.filter().getLabelWF());
        this.labelOrd = new RelationLabelOrder(query.min().getLabelOrder());
    }

    @Override protected Optional<SearchState> doNext() throws MetaborgException {
        if(fired.getAndSet(true)) {
            return Optional.empty();
        }

        // set-up parameters
        final DataWF<ITerm, Collection<CEqual>> dataWF = new DataWF<ITerm, Collection<CEqual>>() {

            @Override public Optional<Collection<CEqual>> wf(ITerm datum)
                    throws ResolutionException, InterruptedException {
                final IConstraint constraint = apply(query.filter().getDataWF(), ImmutableList.of(datum), query);

                // remove all previously created variables/scopes to make them rigid/closed
                final State state = input._1().state().clearVarsAndScopes();
                final SolverResult result = Solver.solve(state, constraint, isComplete, new NullDebugContext());

                if(result.hasErrors()) {
                    return Optional.empty();
                }
                if(!result.delays().keySet().stream().allMatch(c -> c instanceof CEqual)) {
                    return Optional.empty();
                }
                final Collection<CEqual> eqs =
                        result.delays().keySet().stream().map(c -> (CEqual) c).collect(Collectors.toList());
                return Optional.of(eqs);
            }

            @Override public boolean sureThing(Collection<CEqual> x) {
                return x.isEmpty();
            }

        };

        // build name resolution
        final NameResolution<Scope, ITerm, ITerm, Collection<CEqual>> nameResolution;
        // @formatter:off
        nameResolution = NameResolution.<Scope, ITerm, ITerm, Collection<CEqual>>builder()
                .withLabelWF(labelWF)
                .withDataWF(dataWF)
                .withLabelOrder(labelOrd)
                .withDataEquiv(dataEq)
                .withEdgeComplete(isComplete2)
                .withDataComplete(isComplete2)
                .build(scopeGraph, query.relation());
        // @formatter:on

        // resolve
        final Env<Scope, ITerm, ITerm, Collection<CEqual>> env;
        try {
            env = nameResolution.resolve(scope);
        } catch(ResolutionException e) {
            return Optional.empty();
        } catch(InterruptedException e) {
            throw new RuntimeException(e); // FIXME
        }

        // create output
        final List<ITerm> pathTerms =
                env.matches.stream().map(m -> StatixTerms.explicate(m.path)).collect(ImmutableList.toImmutableList());
        final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
        constraints.add(new CEqual(B.newList(pathTerms), query.resultTerm(), query));
        env.matches.forEach(m -> constraints.addAll(m.x));
        constraints.addAll(input._1().constraints());
        final SearchState newState = input._1().update(input._1().state(), constraints.build());
        return Optional.of(newState);

    }

    @Override public String toString() {
        return "resolve-query";
    }

}