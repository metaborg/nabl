package mb.statix.random;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.metaborg.core.MetaborgException;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.random.strategy.SearchStrategies;
import mb.statix.solver.IConstraint;
import mb.statix.solver.persistent.State;
import mb.statix.spec.Spec;

public class RandomTermGenerator2 implements SearchNodes<SearchState> {

    private static final ILogger log = LoggerUtils.logger(RandomTermGenerator2.class);

    private static final boolean DEBUG = false;
    private static final int LINE_WIDTH = 100;
    private static final int MAX_DEPTH = 1024;

    private final long seed = System.currentTimeMillis();
    private final Random rnd = new Random(seed);
    private final Deque<SearchNodes<SearchState>> stack = new LinkedList<>();
    private final SearchStrategy<SearchState, SearchState> search;
    private final SearchStrategy<SearchState, SearchState> infer;
    private final Predicate1<CUser> done;
    private final Function1<ITerm, String> pp;

    public RandomTermGenerator2(Spec spec, IConstraint constraint, SearchStrategy<SearchState, SearchState> search,
            SearchStrategy<SearchState, SearchState> infer, Predicate1<CUser> done, Function1<ITerm, String> pp) {
        this.search = search;
        this.infer = infer;
        this.done = done;
        this.pp = pp;
        initStack(spec, constraint);
        log.info("random seed: {}", seed);
        log.info("search strategy: {}", search);
        log.info("infer strategy: {}", infer);
        log.info("constraint: {}", constraint);
        log.info("max depth: {}", MAX_DEPTH);
    }

    private void initStack(Spec spec, IConstraint constraint) {
        final SearchState initState = SearchState.of(State.of(spec), ImmutableList.of(constraint));
        // It is necessary to start with inference, otherwise we get stuck directly,
        // on, e.g., top-level existentials
        final SearchNodes<SearchState> initNodes = infer.apply(new NullSearchContext(rnd), initState, null);
        stack.push(initNodes);
    }

    final AtomicInteger nodeId = new AtomicInteger();
    private int work = 0;
    private int backtracks = 0;
    private int deadEnds = 0;
    private int hits = 0;

    public Optional<SearchNode<SearchState>> next() throws MetaborgException, InterruptedException {
        final SearchContext ctx = new SearchContext() {

            @Override public Random rnd() {
                return rnd;
            }

            @Override public int nextNodeId() {
                return nodeId.incrementAndGet();
            }

            @Override public void addFailed(SearchNode<SearchState> node) {
                printResult('.', "FAILURE", node, Level.Debug, Level.Debug);
            }

        };
        while(!stack.isEmpty()) {
            final SearchNodes<SearchState> nodes = stack.peek();
            final SearchNode<SearchState> node;
            if((node = nodes.next().orElse(null)) == null) {
                backtracks++;
                stack.pop();
                continue;
            }
            work++;
            if(done(node.output())) {
                printResult('+', "SUCCESS", node, Level.Info, Level.Debug);
                hits++;
                return Optional.of(node);
            } else if(stack.size() >= MAX_DEPTH) {
                printResult('^', "FAILURE", node, Level.Debug, Level.Debug);
                deadEnds++;
                continue;
            }
            final SearchNodes<SearchState> nextNodes =
                    SearchStrategies.seq(search, infer).apply(ctx, node.output(), node);
            //            if(!nextNodes.hasNext()) {
            //                printResult('.', "FAILURE", node, Level.Debug, Level.Debug);
            //                deadEnds++;
            //                continue;
            //            }
            stack.push(nextNodes);
        }
        if(!DEBUG) {
            System.out.println();
        }
        log.info("hits       {}", hits);
        log.info("work       {}", work);
        log.info("dead ends  {}", deadEnds);
        log.info("backtracks {}", backtracks);
        return Optional.empty();
    }

    private boolean done(SearchState state) {
        return state.constraints().stream().allMatch(c -> (c instanceof CUser && done.test((CUser) c)));
    }


    private int summaries = 0;

    private void printResult(char summary, String header, SearchNode<SearchState> node, Level level1, Level level2) {
        if(!DEBUG) {
            if((summaries++ % LINE_WIDTH) == 0) {
                System.out.println();
            }
            System.out.print(summary);
        } else {
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

}