package mb.statix.random;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.predicate.Any;
import mb.statix.random.predicate.IsGen;
import mb.statix.random.predicate.IsType;
import mb.statix.random.predicate.Not;
import mb.statix.random.strategy.Either2;
import mb.statix.random.strategy.SearchStrategies;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private static final ILogger log = LoggerUtils.logger(RandomTermGenerator.class);

    private static final int MAX_DEPTH = 1024;

    private final long seed = System.currentTimeMillis();
    private final Random rnd = new Random(seed);
    private final Deque<Iterator<SearchNode<SearchState>>> stack = new LinkedList<>();
    private final SearchStrategy<SearchState, SearchState> strategy = N.seq(enumerateComb(), infer());
    private final Function1<ITerm, String> pp;

    public RandomTermGenerator(Spec spec, IConstraint constraint, Function1<ITerm, String> pp) {
        initStack(spec, constraint);
        this.pp = pp;
        log.info("random seed: {}", seed);
        log.info("strategy: {}", strategy);
        log.info("constraint: {}", constraint);
        log.info("max depth: {}", MAX_DEPTH);
    }

    private void initStack(Spec spec, IConstraint constraint) {
        final SearchState initState = SearchState.of(State.of(spec), ImmutableList.of(constraint));
        // It is necessary to start with inference, otherwise we get stuck directly,
        // on, e.g., top-level existentials
        final Stream<SearchNode<SearchState>> initNodes = infer().apply(new NullSearchContext(rnd), initState, null);
        stack.push(initNodes.iterator());
    }

    final AtomicInteger nodeId = new AtomicInteger();
    private int work = 0;
    private int backtracks = 0;
    private int deadEnds = 0;
    private int hits = 0;

    public Optional<SearchState> next() throws MetaborgException, InterruptedException {
        final SearchContext ctx = new SearchContext() {

            @Override public Random rnd() {
                return rnd;
            }

            @Override public int nextNodeId() {
                return nodeId.incrementAndGet();
            }

            @Override public void addFailed(SearchNode<SearchState> node) {
                printResult("FAILURE", node, Level.Debug, Level.Debug);
            }

        };
        while(!stack.isEmpty()) {
            final Iterator<SearchNode<SearchState>> nodes = stack.peek();
            if(!nodes.hasNext()) {
                backtracks++;
                stack.pop();
                continue;
            }
            final SearchNode<SearchState> node = nodes.next();
            work++;
            if(done(node.output())) {
                printResult("SUCCESS", node, Level.Info, Level.Debug);
                hits++;
                return Optional.of(node.output());
            } else if(stack.size() >= MAX_DEPTH) {
                ctx.addFailed(node);
                deadEnds++;
                continue;
            }
            final Iterator<SearchNode<SearchState>> nextNodes = strategy.apply(ctx, node.output(), node).iterator();
            if(!nextNodes.hasNext()) {
                ctx.addFailed(node);
                deadEnds++;
                continue;
            }
            stack.push(nextNodes);
        }
        log.info("hits       {}", hits);
        log.info("work       {}", work);
        log.info("dead ends  {}", deadEnds);
        log.info("backtracks {}", backtracks);
        return Optional.empty();
    }

    private static boolean done(SearchState state) {
        return state.constraints().stream()
                .allMatch(c -> c instanceof CInequal || (c instanceof CUser && ((CUser) c).name().startsWith("gen_")));
    }


    private static final SearchStrategies N = new SearchStrategies();

    private static SearchStrategy<SearchState, SearchState> infer() {
        return N.seq(N.infer(), dropAst());
    }

    private static SearchStrategy<SearchState, SearchState> dropAst() {
        return N.drop(CAstId.class, CAstProperty.class);
    }

    // enumerate all possible combinations of solving constraints
    private static SearchStrategy<SearchState, SearchState> enumerateComb() {
        // @formatter:off
        return N.seq(selectConstraint(1), N.match(
            expandExpComb(),
            N.resolve()
        ));
        // @formatter:on
    }

    private static SearchStrategy<SearchState, Either2<FocusedSearchState<CUser>, FocusedSearchState<CResolveQuery>>>
            selectConstraint(int limit) {
        // @formatter:off
        return N.limit(limit, N.alt(
            N.select(CUser.class, new Not<>(new IsGen())),
            N.seq(N.select(CResolveQuery.class, new Any<>()), N.canResolve())
        ));
        // @formatter:on
    }

    private static SearchStrategy<FocusedSearchState<CUser>, SearchState> expandExpComb() {
        // @formatter:off
        return N.expand(ImmutableMap.of(
                "T-Unit", 1,
                "T-Fun",  1,
                "T-Var",  1,
                "T-App",  1,
                "T-Let",  1
        ));
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> search(int ruleLimit) {
        // @formatter:off
        return N.seq(selectConstraint(1), N.match(
            expandExpSearch(ruleLimit),
            N.resolve()
        ));
        // @formatter:on
    }

    private static SearchStrategy<FocusedSearchState<CUser>, SearchState> expandExpSearch(int ruleLimit) {
        // @formatter:off
        return N.limit(ruleLimit, N.expand(ImmutableMap.of(
                "T-Unit", 1,
                "T-Fun",  1,
                "T-Var",  1,
                "T-App",  1,
                "T-Let",  0
        )));
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> fillTypes() {
        // @formatter:off
        return N.seq(
            N.select(CUser.class, new IsType()),
            N.limit(5, N.expand(ImmutableMap.of("T-Unit", 2, "T-Fun", 1)))
        );
        // @formatter:on
    }

    public void printResult(String header, SearchNode<SearchState> node, Level level1, Level level2) {
        log.log(level1, "+--- {} ---+.", header);

        node.output().print(s -> log.log(level1, s), (t, u) -> pp.apply(u.findRecursive(t)));

        log.log(level1, "+~~~ Trace ~~~+.");

        boolean first = true;
        SearchNode<?> traceNode = node;
        do {
            log.log(first ? level1 : level2, "+ * {}", traceNode);
            first = false;
            if(traceNode.output() instanceof SearchState) {
                SearchState state = (SearchState) traceNode.output();
                IUnifier u = state.state().unifier();
                log.log(level2, "+   constraints: {}",
                        Constraints.toString(state.constraints(), t -> pp.apply(u.findRecursive(t))));
            }
        } while((traceNode = traceNode.parent()) != null);

        log.log(level1, "+------------+");
    }

}