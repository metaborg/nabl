package statix.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.EndNamedGoal;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.context.IContext;
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
    @Parameters(paramLabel = "FILE", description = "files to check and evaluate") private List<String> files =
            new ArrayList<>();

    private final Spoofax S;
    private final CLIUtils cli;

    private Statix() throws MetaborgException {
        S = new Spoofax();
        cli = new CLIUtils(S);
    }

    public Void call() throws MetaborgException {
        final ILanguageImpl lang = loadLanguage();
        final IProject project = cli.getOrCreateCWDProject();
        if(!S.contextService.available(lang)) {
            throw new MetaborgException("Cannot create project context.");
        }
        final IContext context = S.contextService.get(project.location(), project, lang);
        final TransformActionContrib evalAction = getAction("Evaluate Test", lang);
        final PrintStream msgStream = System.out;
        final IMessagePrinter messagePrinter =
                new WithLocationStreamMessagePrinter(S.sourceTextService, S.projectService, msgStream);
        for(String file : files) {
            final FileObject resource = S.resourceService.resolve(file);
            final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = parseAndAnalyze(resource, context, messagePrinter);
            if(!maybeAnalysisUnit.isPresent()) {
                continue;
            }
            final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
            final IStrategoTerm ast = analysisUnit.ast();
            if(ast == null || !Tools.isTermAppl(ast)
                    || !Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
                continue;
            }
            messagePrinter.print(
                    new Message("Evaluating test.", MessageSeverity.NOTE, MessageType.INTERNAL, resource, null, null),
                    false);
            final String typing = transform(analysisUnit, evalAction, context);
            msgStream.println(typing);
        }
        return null;
    }

    private ILanguageImpl loadLanguage() throws MetaborgException {
        final FileObject langResource;
        try {
            langResource = S.resourceService.resolve("res:statix.lang.spoofax-language");
        } catch(MetaborgRuntimeException ex) {
            throw new MetaborgException("Failed to load language.", ex);
        }
        return cli.loadLanguage(langResource);
    }

    private Optional<ISpoofaxAnalyzeUnit> parseAndAnalyze(FileObject resource, IContext context,
            IMessagePrinter messagePrinter) throws MetaborgException {
        final ILanguageImpl lang = context.language();
        if(!S.syntaxService.available(lang)) {
            throw new MetaborgException("Parsing not available.");
        }
        final String text;
        try {
            text = S.sourceTextService.text(resource);
        } catch(IOException e) {
            throw new MetaborgException("Cannot find " + resource, e);
        }
        final ISpoofaxInputUnit inputUnit = S.unitService.inputUnit(resource, text, lang, null);
        final ISpoofaxParseUnit parseUnit = S.syntaxService.parse(inputUnit);
        if(!parseUnit.valid()) {
            throw new MetaborgException("Parsing failed.");
        }
        for(IMessage message : parseUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!parseUnit.success()) {
            logger.info("{} has syntax errors", resource);
            return Optional.empty();
        }
        if(!S.analysisService.available(lang) || !S.contextService.available(lang)) {
            throw new MetaborgException("Analysis not available.");
        }
        final ISpoofaxAnalyzeUnit analysisUnit;
        try(IClosableLock lock = context.write()) {
            analysisUnit = S.analysisService.analyze(parseUnit, context).result();
        }
        if(!analysisUnit.valid()) {
            throw new MetaborgException("Analysis failed.");
        }
        for(IMessage message : analysisUnit.messages()) {
            messagePrinter.print(message, false);
        }
        if(!analysisUnit.success()) {
            logger.info("{} has type errors.", resource);
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

    private String transform(ISpoofaxAnalyzeUnit analysisUnit, TransformActionContrib action, IContext context)
            throws MetaborgException {
        final ITransformConfig config = new TransformConfig(true);
        final ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit> transformUnit =
                S.transformService.transform(analysisUnit, context, action, config);
        if(!transformUnit.valid()) {
            throw new MetaborgException("Failed to transform " + analysisUnit.source());
        }
        final String details = S.strategoCommon.toString(transformUnit.ast());
        return details;
    }

    // CLI stuff

    public static void main(String... args) {
        try {
            final CommandLine cmd = new CommandLine(new Statix());
            cmd.parseWithHandlers(new RunLast().andExit(0), CommandLine.defaultExceptionHandler().andExit(1), args);
        } catch(MetaborgException e) {
            logger.error("", e);
        }
    }

}
