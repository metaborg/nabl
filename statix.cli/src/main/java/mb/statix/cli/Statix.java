package mb.statix.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.context.IContext;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.messages.IMessage;
import org.metaborg.core.messages.IMessagePrinter;
import org.metaborg.core.messages.WithLocationStreamMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.transform.ITransformConfig;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Iterables;

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

    Spoofax S;
    private CLIUtils cli;
    ILanguageImpl lang;
    IProject project;
    IContext context;
    PrintStream msgStream = System.out;
    IMessagePrinter messagePrinter;

    public Statix() throws MetaborgException {
        S = new Spoofax(new StatixCLIModule());
        cli = new CLIUtils(S);
        lang = loadLanguage();
        project = cli.getOrCreateCWDProject();
        context = S.contextService.get(project.location(), project, lang);
        msgStream = System.out;
        messagePrinter = new WithLocationStreamMessagePrinter(S.sourceTextService, S.projectService, msgStream);
    }

    @Command(name = "test") public void
            test(@Parameters(paramLabel = "FILE", description = "Statix test file to evaluate") String file)
                    throws MetaborgException {
        new StatixTest(this).run(file);
    }

    @Command(name = "generate") public void
            generate(@Parameters(paramLabel = "FILE", description = "Statix test file to generate from") String file)
                    throws MetaborgException {
        new StatixGenerate(this).run(file);
    }

    @Command(name = "repl") public void
            repl(@Parameters(paramLabel = "FILE", description = "Statix file to load") String file)
                    throws MetaborgException, IOException {
        new StatixRepl(this).run(file);
    }

    private ILanguageImpl loadLanguage() throws MetaborgException {
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
            langResource = S.resourceService.resolve("res:statix.lang.spoofax-language");
        } catch(MetaborgRuntimeException ex) {
            throw new MetaborgException("Failed to load language.", ex);
        }
        lang = cli.loadLanguage(langResource);
        if(lang != null) {
            return lang;
        }
        throw new MetaborgException("Failed to load language from path or resources.");
    }

    Optional<ISpoofaxAnalyzeUnit> loadFile(String file) throws MetaborgException {
        final FileObject resource = S.resourceService.resolve(file);
        final String text;
        try {
            text = S.sourceTextService.text(resource);
        } catch(IOException e) {
            throw new MetaborgException("Cannot find " + file, e);
        }
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, text, lang, null);
        final Optional<ISpoofaxParseUnit> parseUnit = parse(inputUnit);
        if(!parseUnit.isPresent()) {
            return Optional.empty();
        }
        final Optional<ISpoofaxAnalyzeUnit> analysisUnit = analyze(parseUnit.get());
        return analysisUnit;
    }

    Optional<ISpoofaxParseUnit> parse(ISpoofaxInputUnit inputUnit) throws MetaborgException {
        final ILanguageImpl lang = context.language();
        if(!S.syntaxService.available(lang)) {
            throw new MetaborgException("Parsing not available.");
        }
        final ISpoofaxParseUnit parseUnit = S.syntaxService.parse(inputUnit);
        for(IMessage message : parseUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!parseUnit.valid()) {
            throw new MetaborgException("Parsing failed.");
        }
        if(!parseUnit.success()) {
            logger.info("{} has syntax errors", inputUnit.source());
            return Optional.empty();
        }
        return Optional.of(parseUnit);
    }

    Optional<ISpoofaxAnalyzeUnit> analyze(ISpoofaxParseUnit parseUnit) throws MetaborgException {
        if(!S.analysisService.available(lang)) {
            throw new MetaborgException("Analysis not available.");
        }
        final ISpoofaxAnalyzeUnit analysisUnit;
        try(IClosableLock lock = context.write()) {
            analysisUnit = S.analysisService.analyze(parseUnit, context).result();
        }
        for(IMessage message : analysisUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!analysisUnit.valid()) {
            throw new MetaborgException("Analysis failed.");
        }
        if(!analysisUnit.success()) {
            logger.info("{} has type errors.", parseUnit.source());
        }
        return Optional.of(analysisUnit);
    }

    TransformActionContrib getAction(String name, ILanguageImpl lang) throws MetaborgException {
        final ITransformGoal goal = new EndNamedGoal(name);
        if(!S.actionService.available(lang, goal)) {
            throw new MetaborgException("Cannot find transformation " + name);
        }
        final TransformActionContrib action;
        try {
            action = Iterables.getOnlyElement(S.actionService.actionContributions(lang, goal));
        } catch(NoSuchElementException ex) {
            throw new MetaborgException("Transformation " + name + " not a singleton.");
        }
        return action;
    }

    IStrategoTerm transform(ISpoofaxAnalyzeUnit analysisUnit, TransformActionContrib action) throws MetaborgException {
        final ITransformConfig config = new TransformConfig(true);
        final ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit> transformUnit =
                S.transformService.transform(analysisUnit, context, action, config);
        for(IMessage message : transformUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!transformUnit.valid()) {
            throw new MetaborgException("Failed to transform " + transformUnit.source());
        }
        return transformUnit.ast();
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
