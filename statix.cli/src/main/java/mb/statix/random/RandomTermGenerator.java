package mb.statix.random;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import mb.nabl2.terms.ITerm;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CUser;
import mb.statix.random.predicate.Any;
import mb.statix.random.predicate.IsType;
import mb.statix.random.predicate.Not;
import mb.statix.random.strategy.SearchStrategies;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator {

    private static final ILogger log = LoggerUtils.logger(RandomTermGenerator.class);

    private static final int SIZE = 10;

    private final long seed = System.currentTimeMillis();
    private final Random rnd = new Random(seed);
    private final Deque<Iterator<SearchNode<SearchState>>> stack = new LinkedList<>();
    private final SearchStrategy<SearchState, SearchState> strategy = N.seq(enumerate(), infer());
    private final Function1<ITerm, String> pp;

    public RandomTermGenerator(Spec spec, IConstraint constraint, Function1<ITerm, String> pp) {
        initStack(spec, constraint);
        this.pp = pp;
    }

    private void initStack(Spec spec, IConstraint constraint) {
        final SearchState initState = SearchState.of(State.of(spec), ImmutableList.of(constraint));
        // It is necessary to start with inference, otherwise we get stuck directly,
        // on, e.g., top-level existentials
        final Stream<SearchNode<SearchState>> initNodes =
                infer().apply(new NullSearchContext(rnd), SIZE, initState, null);
        stack.push(initNodes.iterator());
    }

    private int work = 0;
    private int backtracks = 0;
    private int deadEnds = 0;
    private int hits = 0;
    private int maxDepth = 0;

    public Optional<SearchState> next() throws MetaborgException, InterruptedException {
        final SearchContext ctx = new SearchContext() {
            @Override public Random rnd() {
                return rnd;
            }

            @Override public void addFailed(SearchNode<SearchState> node) {
//                printResult("FAILURE", node);
            }
        };
        while(!stack.isEmpty()) {
            final Iterator<SearchNode<SearchState>> nodes = stack.peek();
            maxDepth = Math.max(maxDepth, stack.size());
            if(!nodes.hasNext()) {
                backtracks++;
                stack.pop();
                continue;
            }
            final SearchNode<SearchState> node = nodes.next();
            if(node.size() <= 0) {
                ctx.addFailed(node);
                deadEnds++;
                continue;
            }
            if(done(node.output())) {
                printResult("SUCCESS", node);
                hits++;
                return Optional.of(node.output());
            }
            final Iterator<SearchNode<SearchState>> nextNodes =
                    strategy.apply(ctx, node.size(), node.output(), node).iterator();
            work++;
            if(!nextNodes.hasNext()) {
                deadEnds++;
                continue;
            }
            stack.push(nextNodes);
        }
        log.info("hits       {}", hits);
        log.info("dead ends  {}", deadEnds);
        log.info("work       {}", work);
        log.info("backtracks {}", backtracks);
        log.info("max depth  {}", maxDepth);
        return Optional.empty();
    }

    private static boolean done(SearchState state) {
        return state.constraints().stream().allMatch(c -> c instanceof CInequal);
    }


    private static final SearchStrategies N = new SearchStrategies();

    private static SearchStrategy<SearchState, SearchState> infer() {
        return N.seq(N.infer(), dropAst());
    }

    private static SearchStrategy<SearchState, SearchState> dropAst() {
        return N.drop(CAstId.class, CAstProperty.class);
    }

    private static SearchStrategy<SearchState, SearchState> search1a() {
        // @formatter:off
        return N.limit(15, N.alt(
            N.seq(
                N.limit(10, N.select(CUser.class, new Not<>(new IsType()))),
                N.limit(10, N.expand())
            ), 
            N.seq(
                N.limit(10, N.select(CResolveQuery.class, new Any<>())),
                N.limit(10, N.resolve())
            )
        ));
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> search1c(int size) {
        // @formatter:off
        return N.atSize(
            10   , fillTypes(),
            size , search1a()
        );
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> search1b() {
        // @formatter:off
        return N.limit(15, N.alt(
            N.limit(50, N.seq(
                N.select(CUser.class, new Not<>(new IsType())),
                N.expand()
            )), 
            N.limit(50, N.seq(
                N.select(CResolveQuery.class, new Any<>()),
                N.resolve()
            ))
        ));
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> search2(int size) {
        // @formatter:off
        return N.atSize(
            size, N.seq(
                N.select(CUser.class, new Not<>(new IsType())),
                N.limit(4, N.expand())
            ),
            size/2, N.seq(N.select(CResolveQuery.class, new Any<>()), N.resolve()),
            size/4, N.limit(2,
                N.seq(
                    N.select(CUser.class, new IsType()),
                    N.expand()
                )
            )
        );
        // @formatter:on
    }

    private static SearchStrategy<SearchState, SearchState> search3(int size) {
        // @formatter:off
        return N.atSize(
            size, N.limit(15, N.alt(
                N.seq(N.limit(10, N.select(CUser.class, new Not<>(new IsType()))), N.limit(10, N.expand())), 
                N.seq(N.limit(10, N.select(CResolveQuery.class, new Any<>())), N.limit(10, N.resolve()))
            )),
            size/2, fillTypes()
        );
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

    private static SearchStrategy<SearchState, SearchState> enumerate() {
        // @formatter:off
        return N.alt(
            N.seq(N.select(CUser.class, new Any<>()), N.expand()), 
            N.seq(N.select(CResolveQuery.class, new Any<>()), N.resolve())
        );
        // @formatter:on
    }

    public void printResult(String header, SearchNode<SearchState> node) {
        log.info("+--- {} ---+.", header);

        node.output().print(log::info, (t, u) -> pp.apply(u.findRecursive(t)));

        log.info("+~~~ Trace ~~~+.");

        SearchNode<?> traceNode = node;
        while(traceNode != null) {
            log.info("+ * {}", traceNode);
            traceNode = traceNode.parent();
        }

        log.info("+------------+");
    }

}