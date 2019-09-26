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
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Lists;

import mb.nabl2.terms.ITerm;
import mb.statix.random.SearchState;
import mb.statix.random.spoofax.StatixGenerator;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private static final String VAR = "e";

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException, InterruptedException {
        final FileObject resource = STX.S.resolve(file);
        final StatixGenerator statixGen = new StatixGenerator(STX.S, STX.project, resource, Paret.search());

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
        final List<SearchState> results = Lists.newArrayList(statixGen);
        results.forEach(s -> {
            System.out.println(pretty.apply(s));
        });
        log.info("Generated {} random terms.", results.size());

    }

}