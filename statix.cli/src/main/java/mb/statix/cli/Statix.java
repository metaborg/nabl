package mb.statix.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.CompileGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageFileSelector;
import org.metaborg.core.messages.WithLocationStreamMessagePrinter;
import org.metaborg.core.processing.ITask;
import org.metaborg.core.project.IProject;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.RunLast;
import picocli.CommandLine.Spec;

@Command(name = "java -jar statix.jar", description = "Type check and evaluate Statix files.", separator = "=")
public class Statix {

    private static final ILogger logger = LoggerUtils.logger(Statix.class);

    @Spec private CommandSpec spec;
    @Option(names = { "-h", "--help" }, description = "show usage help", usageHelp = true) private boolean usageHelp;

    final Spoofax S;
    final CLIUtils cli;
    final ILanguageImpl stxLang;
    final IProject project;
    final IContext context;
    final PrintStream msgStream;

    public Statix() throws MetaborgException {
        S = new Spoofax(new StatixCLIModule());
        cli = new CLIUtils(S);
        stxLang = loadStxLang();
        project = cli.getOrCreateProject(findProjectDir(cli.getCWD()));
        context = S.contextService.get(project.location(), project, stxLang);
        msgStream = System.out;
    }

    @Command(name = "test") public void
            test(@Parameters(paramLabel = "FILE", description = "Statix test file to evaluate") String file)
                    throws MetaborgException {
        new StatixTest(this).run(file);
    }

    @Command(name = "generate") public void
            generate(@Parameters(paramLabel = "FILE", description = "Statix test file to generate from") String file)
                    throws MetaborgException, InterruptedException {
        new StatixGenerate(this).run(file);
    }

    @Command(name = "repl") public void
            repl(@Parameters(paramLabel = "FILE", description = "Statix file to load") String file)
                    throws MetaborgException, IOException {
        new StatixRepl(this).run(file);
    }

    private ILanguageImpl loadStxLang() throws MetaborgException {
        ILanguageImpl lang;
        // try loading from path
        cli.loadLanguagesFromPath();
        ILanguage L = S.languageService.getLanguage("StatixLang");
        lang = L != null ? L.activeImpl() : null;
        if(lang != null) {
            return lang;
        }
        // try loading from resources
        final FileObject langResource;
        try {
            langResource = S.resourceService.resolve("res://statix.lang.spoofax-language");
        } catch(MetaborgRuntimeException ex) {
            throw new MetaborgException("Failed to load language.", ex);
        }
        lang = cli.loadLanguage(langResource);
        if(lang != null) {
            return lang;
        }
        throw new MetaborgException("Failed to load language from path or resources.");
    }

    Optional<ISpoofaxAnalyzeUnit> loadStxFile(FileObject resource) throws MetaborgException {
        final ITask<ISpoofaxBuildOutput> task;
        try {
            final BuildInputBuilder inputBuilder = new BuildInputBuilder(project);
            // @formatter:off
            final BuildInput input = inputBuilder
                .withDefaultIncludePaths(true)
                .withSourcesFromDefaultSourceLocations(true)
                .withSelector(new LanguageFileSelector(S.languageIdentifierService, stxLang))
                .withMessagePrinter(new WithLocationStreamMessagePrinter(S.sourceTextService, S.projectService, msgStream))
                .withThrowOnErrors(true)
                .addTransformGoal(new CompileGoal())
                .build(S.dependencyService, S.languagePathService);
            // @formatter:on
            task = S.processorRunner.build(input, null, null).schedule().block();
        } catch(MetaborgException | InterruptedException e) {
            throw new MetaborgException("Building Statix files failed unexpectedly", e);
        } catch(MetaborgRuntimeException e) {
            throw new MetaborgException("Building Statix files failed", e);
        }

        final ISpoofaxBuildOutput output = task.result();
        if(!output.success()) {
            return Optional.empty();
        }

        return output.analysisResults().stream()
            .filter(r -> r.source().getName().equals(resource.getName())).findFirst();
    }

    private FileObject findProjectDir(FileObject resource) throws MetaborgException {
        final FileObject init;
        try {
            init = (resource.isFolder() ? resource : resource.getParent());
        } catch(FileSystemException e) {
            logger.error("Error while searching for project configuration.", e);
            throw new MetaborgException(e);
        }
        try {
            FileObject dir = init;
            while(dir != null) {
                FileObject config = dir.resolveFile(MetaborgConstants.FILE_CONFIG);
                if(config != null && config.exists()) {
                    return dir;
                }
                dir = dir.getParent();
            }
        } catch(FileSystemException e) {
            logger.error("Error while searching for project configuration.", e);
        }
        return init;
    }

    String format(IStrategoTerm term) throws MetaborgException {
        return S.strategoCommon.toString(term);
    }

    public static void main(String... args) {
        CommandLine cmd;
        try {
            cmd = new CommandLine(new Statix());
            cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
        } catch(MetaborgException e) {
            logger.error("Cannot initialize Statix CLI", e);
            System.exit(1);
        }
    }

}
