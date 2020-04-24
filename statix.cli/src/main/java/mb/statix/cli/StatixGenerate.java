package mb.statix.cli;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.shell.StatixGenerator;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.TermFormatter;
import mb.statix.generator.RandomTermGenerator;
import mb.statix.generator.SearchLogger;
import mb.statix.generator.SearchState;
import mb.statix.generator.SearchStrategy;
import mb.statix.generator.nodes.SearchElement;
import mb.statix.generator.nodes.SearchNode;
import mb.statix.generator.nodes.SearchNodes;
import mb.statix.generator.util.StreamProgressPrinter;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private static final boolean DEBUG = true;
    private static final boolean TRACE = false;
    private static final String VAR = "e";
    private static final int COUNT = 2 * 42;

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException, InterruptedException {
        final FileObject resource = STX.S.resolve(file);

        TermFormatter tf = ITerm::toString;
        try {
            final ILanguageImpl lang = STX.cli.loadLanguage(STX.project.location());
            final IContext context = STX.S.contextService.get(resource, STX.project, lang);
            tf = StatixGenerator.pretty(STX.S, context, "pp-generated");
        } catch(MetaborgException e) {
            // ignore
        }
        final TermFormatter _tf = tf;
        final Function1<SearchState, String> pretty = (s) -> _tf.format(project(VAR, s));

        final DescriptiveStatistics hitStats = new DescriptiveStatistics();
        final DescriptiveStatistics missStats = new DescriptiveStatistics();
        final StreamProgressPrinter progress = new StreamProgressPrinter(System.err, 80, out -> {
            long hits = hitStats.getN();
            long all = hits + missStats.getN();
            out.println(" " + hits + "/" + all + " " + summary(hitStats));
        });
        final SearchLogger<SearchState, SearchState> searchLog = new SearchLogger<SearchState, SearchState>() {

            @Override public void init(long seed, SearchStrategy<SearchState, SearchState> strategy, Iterable<IConstraint> constraints) {
                log.info("seed {}", seed);
                log.info("strategy {}", strategy);
                log.info("constraints {}", constraints);
            }

            @Override public void success(SearchNode<SearchState> n) {
                progress.step('+');
                addSize(n.output(), hitStats);
                logSuccess(log, Level.Debug, n, pretty);
            }

            @Override public void failure(SearchNodes<?> nodes) {
                progress.step('.');
                SearchNode<?> parentNode = nodes.parent();
                if (parentNode != null && parentNode.output() instanceof SearchState) {
                    addSize((SearchState)parentNode.output(), missStats);
                }
                logFailure(log, Level.Debug, nodes, pretty);
            }

            private void addSize(SearchState s, DescriptiveStatistics stats) {
                s.state().unifier().size(project(VAR, s)).ifFinite(size -> {
                    stats.addValue(size.doubleValue());
                });
            }

        };

        final StatixGenerator statixGen = new StatixGenerator(STX.S, STX.context, resource);
        final Spec spec = statixGen.spec(); // Paret.addFragments(statixGen.spec());
        final RandomTermGenerator rtg =
                new RandomTermGenerator(spec, statixGen.constraint(), new Paret(spec).search(), searchLog);
        final Stream<SearchState> resultStream = rtg.apply().nodes().map(sn -> {
            searchLog.success(sn);
            return sn.output();
        });

        log.info("Generating random terms.");
        final List<SearchState> results = Lists.newArrayList(resultStream.limit(COUNT).iterator());
        progress.done();
        results.forEach(s -> {
            System.out.println(pretty.apply(s));
        });
        log.info("Generated {} random terms.", results.size());
        logStatsInfo("hits", hitStats);
        logStatsInfo("misses", missStats);
    }

    private static void logStatsInfo(String name, DescriptiveStatistics stats) {
        log.info("{} {} of sizes {} (max/P80/P60/P40/P20/min)", name, stats.getN(), summary(stats));
    }

    private static String summary(DescriptiveStatistics stats) {
        return String.format("%.1f/%.1f/%.1f/%.1f/%.1f/%.1f", stats.getMax(), stats.getPercentile(80),
                stats.getPercentile(60), stats.getPercentile(40), stats.getPercentile(20), stats.getMin());
    }

    private static void logSuccess(ILogger log, Level lvl, SearchNode<SearchState> node,
            Function1<SearchState, String> pp) {
        if(!DEBUG) {
            return;
        }
        log.log(lvl, "=== SUCCESS ===");
        log.log(lvl, " * {}", pp.apply(node.output()));
        log.log(lvl, "---- Trace ----");
        logTrace(log, lvl, node, 1, pp);
        log.log(lvl, "===============");
    }

    private static void logFailure(ILogger log, Level lvl, SearchElement node, Function1<SearchState, String> pp) {
        if(!DEBUG) {
            return;
        }
        log.log(lvl, "=== FAILURE ===");
        logTrace(log, lvl, node, Integer.MAX_VALUE, pp);
        log.log(lvl, "===============");
    }

    @SuppressWarnings("unused") private static void logTrace(ILogger log, Level lvl, SearchElement node, int maxDepth,
            Function1<SearchState, String> pp) {
        if(node instanceof SearchNodes) {
            SearchNodes<?> nodes = (SearchNodes<?>) node;
            log.log(lvl, " * {}", nodes.desc());
            logTrace(log, lvl, nodes.parent(), maxDepth, pp);
        } else {
            SearchNode<?> traceNode = (SearchNode<?>) node;
            int depth = 0;
            do {
                log.log(lvl, " * [{}] {}", traceNode.id(), traceNode.desc());
                if((depth++ == 0 || (TRACE && depth <= maxDepth)) && traceNode.output() instanceof SearchState) {
                    SearchState state = (SearchState) ((SearchNode<?>) traceNode).output();
                    state.print(ln -> log.log(lvl, "   {}", ln), (t, u) -> u.toString(t));
                }
            } while((traceNode = traceNode.parent()) != null);
            log.log(lvl, " # depth {}", depth);
        }
    }

    private static ITerm project(String varName, SearchState s) {
        final ITermVar v = B.newVar("", varName);
        if(s.existentials().containsKey(v)) {
            return s.state().unifier().findRecursive(s.existentials().get(v));
        } else {
            return v;
        }
    }

}