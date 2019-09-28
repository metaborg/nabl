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
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.random.spoofax.StatixGenerator;
import mb.statix.random.util.StreamProgressPrinter;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private static final String VAR = "e";

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException, InterruptedException {
        final FileObject resource = STX.S.resolve(file);
        final StreamProgressPrinter progress = new StreamProgressPrinter(System.err, 100);
        final StatixGenerator statixGen = new StatixGenerator(STX.S, STX.context, resource, Paret.search(), progress);

        final Function1<SearchState, String> pretty;
        final Optional<FileObject> maybeProject = STX.findProject(resource);
        if(maybeProject.isPresent()) {
            final IProject project = STX.cli.getOrCreateProject(maybeProject.get());
            final ILanguageImpl lang = STX.cli.loadLanguage(project.location());
            final IContext context = STX.S.contextService.get(resource, project, lang);
            pretty = statixGen.pretty(context, VAR, "pp-generated");
        } else {
            pretty = statixGen.project(VAR, ITerm::toString);
        }

        log.info("Generating random terms.");
        final List<SearchState> results = Lists.newArrayList(statixGen.apply().map(sn -> {
//            logTrace(sn, Level.Debug, pretty);
            return sn;
        }).limit(100).iterator());
        progress.done();
        results.forEach(s -> {
            System.out.println(pretty.apply(s));
        });
        log.info("Generated {} random terms.", results.size());

    }

    private static void logTrace(SearchNode<SearchState> node, Level level, Function1<SearchState, String> pp) {
        log.log(level, "=== Program ===");
        log.log(level, " * {}", pp.apply(node.output()));
        log.log(level, "---- Trace ----");
        SearchNode<?> traceNode = node;
        do {
            log.log(level, " * [{}] {}", traceNode.id(), traceNode.desc());
            if(traceNode.output() instanceof SearchState) {
                SearchState state = (SearchState) traceNode.output();
                state.print(ln -> log.log(level, "   {}", ln), (t, u) -> u.toString(t));
            }
        } while((traceNode = traceNode.parent()) != null);
        log.log(level, "===============");

    }

}