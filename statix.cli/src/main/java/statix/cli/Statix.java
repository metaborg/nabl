package statix.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.vfs2.FileObject;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
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
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.core.messages.WithLocationStreamMessagePrinter;
import org.metaborg.core.project.IProject;
import org.metaborg.core.transform.ITransformConfig;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.shell.CLIUtils;
import org.metaborg.spoofax.core.syntax.JSGLRParserConfiguration;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
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
public class Statix implements Callable<Void> {

    private static final ILogger logger = LoggerUtils.logger(Statix.class);

    @Spec private CommandSpec spec;
    @Option(names = { "-h", "--help" }, description = "show usage help", usageHelp = true) private boolean usageHelp;
    @Option(names = { "-i", "--interactive" }, description = "interactive mode") private boolean interactive;
    @Parameters(paramLabel = "FILE", description = "files to check and evaluate") private String file;

    private Spoofax S;
    private CLIUtils cli;
    private ILanguageImpl lang;
    private IProject project;
    private IContext context;
    private IMessagePrinter messagePrinter;
    private TransformActionContrib evalAction;

    @Override public Void call() throws MetaborgException, IOException {
        S = new Spoofax();
        cli = new CLIUtils(S);
        lang = loadLanguage();
        project = cli.getOrCreateCWDProject();
        if(!S.contextService.available(lang)) {
            throw new MetaborgException("Cannot create project context.");
        }
        context = S.contextService.get(project.location(), project, lang);
        final PrintStream msgStream = System.out;
        messagePrinter = new WithLocationStreamMessagePrinter(S.sourceTextService, S.projectService, msgStream);
        evalAction = getAction("Evaluate Test", lang);
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = loadFile(file);
        if(!maybeAnalysisUnit.isPresent()) {
            return null;
        }
        final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
        final IStrategoTerm ast = analysisUnit.ast();
        if(ast != null && Tools.isTermAppl(ast) && Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
            messagePrinter.print(new Message("Evaluating test.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
            final String typing = transform(analysisUnit, evalAction);
            msgStream.println(typing);
        }
        if(interactive) {
            repl();
        }
        return null;
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

    private Optional<ISpoofaxAnalyzeUnit> loadFile(String file) throws MetaborgException {
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

    private Optional<ISpoofaxParseUnit> parse(ISpoofaxInputUnit inputUnit) throws MetaborgException {
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

    private Optional<ISpoofaxAnalyzeUnit> analyze(ISpoofaxParseUnit parseUnit) throws MetaborgException {
        if(!S.analysisService.available(lang) || !S.contextService.available(lang)) {
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

    private TransformActionContrib getAction(String name, ILanguageImpl lang) throws MetaborgException {
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

    private String transform(ISpoofaxAnalyzeUnit analysisUnit, TransformActionContrib action) throws MetaborgException {
        final ITransformConfig config = new TransformConfig(true);
        final ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit> transformUnit =
                S.transformService.transform(analysisUnit, context, action, config);
        for(IMessage message : transformUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!transformUnit.valid()) {
            throw new MetaborgException("Failed to transform " + analysisUnit.source());
        }
        final String details = S.strategoCommon.toString(transformUnit.ast());
        return details;
    }

    private void repl() throws IOException, MetaborgException {
        final Terminal terminal = TerminalBuilder.builder().build();
        final LineReader reader =
                LineReaderBuilder.builder().terminal(terminal).option(LineReader.Option.AUTO_FRESH_LINE, true).build();
        final ILanguageImpl lang = context.language();
        final JSGLRParserConfiguration config = new JSGLRParserConfiguration("CommandLine");
        while(true) {
            final String line;
            try {
                line = reader.readLine("> ");
            } catch(UserInterruptException e) {
                continue;
            } catch(EndOfFileException e) {
                return;
            }
            final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(line, lang, null, config);
            final Optional<ISpoofaxParseUnit> maybeParseUnit = parse(inputUnit);
            if(!maybeParseUnit.isPresent()) {
                continue;
            }
            final ISpoofaxParseUnit parseUnit = maybeParseUnit.get();
            terminal.writer().println(S.strategoCommon.toString(parseUnit.ast()));
            final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = analyze(parseUnit);
            if(!maybeAnalysisUnit.isPresent()) {
                continue;
            }
            final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
            final String typing = transform(analysisUnit, evalAction);
            terminal.writer().println(typing);
        }
    }

    public static void main(String... args) {
        final CommandLine cmd = new CommandLine(new Statix());
        cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
    }

}
