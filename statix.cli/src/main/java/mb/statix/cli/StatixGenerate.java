package mb.statix.cli;

import java.util.List;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.statix.random.SearchLogger;
import mb.statix.random.SearchState;
import mb.statix.random.SearchStrategy;
import mb.statix.random.nodes.SearchElement;
import mb.statix.random.nodes.SearchNode;
import mb.statix.random.nodes.SearchNodes;
import mb.statix.random.spoofax.StatixGenerator;
import mb.statix.random.util.StreamProgressPrinter;
import mb.statix.solver.IConstraint;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private static final boolean DEBUG = true;
    private static final boolean TRACE = false;
    private static final String VAR = "e";

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException, InterruptedException {
        final FileObject resource = STX.S.resolve(file);

        final Function1<SearchState, String> pretty;
        final Optional<FileObject> maybeProject = STX.findProject(resource);
        if(maybeProject.isPresent()) {
            final IProject project = STX.cli.getOrCreateProject(maybeProject.get());
            final ILanguageImpl lang = STX.cli.loadLanguage(project.location());
            final IContext context = STX.S.contextService.get(resource, project, lang);
            pretty = StatixGenerator.pretty(STX.S, context, VAR, "pp-generated");
        } else {
            pretty = StatixGenerator.project(VAR, ITerm::toString);
        }

        final StreamProgressPrinter progress = new StreamProgressPrinter(System.err, 100);
        final SearchLogger searchLog = new SearchLogger() {

            @Override public void init(long seed, SearchStrategy<?, ?> strategy, Iterable<IConstraint> constraints) {
                log.info("seed {}", seed);
                log.info("strategy {}", strategy);
                log.info("constraints {}", constraints);
            }

            @Override public void success(SearchNode<SearchState> n) {
                progress.step('+');
                logSuccess(log, Level.Debug, n, pretty::apply);
            }

            @Override public void failure(SearchElement e) {
                progress.step('.');
                logFailure(log, Level.Debug, e, pretty::apply);
            }

        };
        final StatixGenerator statixGen = new StatixGenerator(STX.S, STX.context, resource, Paret.search(), searchLog);

        log.info("Generating random terms.");
        final List<SearchState> results = Lists.newArrayList(statixGen.apply().limit(42).iterator());
        progress.done();
        results.forEach(s -> {
            System.out.println(pretty.apply(s));
        });
        log.info("Generated {} random terms.", results.size());

    }

    private static void logSuccess(ILogger log, Level lvl, SearchNode<SearchState> node,
            Function1<SearchState, String> pp) {
        if(!DEBUG) {
            return;
        }
        log.log(lvl, "=== SUCCESS ===");
        log.log(lvl, " * {}", pp.apply(node.output()));
        log.log(lvl, "---- Trace ----");
        logTrace(log, lvl, node, pp);
        log.log(lvl, "===============");
    }

    private static void logFailure(ILogger log, Level lvl, SearchElement node, Function1<SearchState, String> pp) {
        if(!DEBUG) {
            return;
        }
        log.log(lvl, "=== FAILURE ===");
        logTrace(log, lvl, node, pp);
        log.log(lvl, "===============");
    }

    private static void logTrace(ILogger log, Level lvl, SearchElement node, Function1<SearchState, String> pp) {
        if(node instanceof SearchNodes) {
            SearchNodes<?> nodes = (SearchNodes<?>) node;
            if(!nodes.success()) {
                log.log(lvl, " * {}", nodes.error());
            }
            logTrace(log, lvl, nodes.parent(), pp);
        } else {
            int depth = 0;
            SearchNode<?> traceNode = (SearchNode<?>) node;
            do {
                log.log(lvl, " * [{}] {}", traceNode.id(), traceNode.desc());
                if((++depth == 1 || TRACE) && traceNode.output() instanceof SearchState) {
                    SearchState state = (SearchState) ((SearchNode<?>) traceNode).output();
                    state.print(ln -> log.log(lvl, "   {}", ln), (t, u) -> u.toString(t));
                }
            } while((traceNode = traceNode.parent()) != null);
            log.log(lvl, " # depth {}", depth);
        }
    }

}