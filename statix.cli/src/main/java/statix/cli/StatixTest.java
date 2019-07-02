package statix.cli;

import java.io.PrintStream;
import java.util.NoSuchElementException;

import org.metaborg.core.MetaborgException;
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
import org.metaborg.core.transform.ITransformConfig;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Iterables;

/**
 * Class for executing statix test evaluations.
 */
public class StatixTest {
    private Spoofax S;
    private IContext context;
    private IMessagePrinter printer;
    private TransformActionContrib evalAction;
    private PrintStream msgStream;
    
    public StatixTest(Spoofax S, IContext context, IMessagePrinter printer, PrintStream msgStream) throws MetaborgException {
        this.S = S;
        this.context = context;
        this.printer = printer;
        this.msgStream = msgStream;
        this.evalAction = getAction("Evaluate Test", context.language());
    }
    
    public void evaluateTest(ISpoofaxAnalyzeUnit analysisUnit) throws MetaborgException {
        final IStrategoTerm ast = analysisUnit.ast();
        
        //TODO implement separated tests / just spec analysis, IDEA: load spec only once (?)
        if (ast != null && Tools.isTermAppl(ast) && Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
            printer.print(new Message("Evaluating test.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
            final String typing = transform(analysisUnit, evalAction);
            msgStream.println(typing);
        }
    }
    
    /**
     * Applies the given transformation action to the given analysis unit.
     * 
     * @param analysisUnit
     *      the analysis unit to apply the action on
     * @param action
     *      the action to apply
     * 
     * @return
     *      the result of the transformation, pretty printed string of the resulting ast
     * 
     * @throws MetaborgException
     *      If the transformation failed.
     */
    private String transform(ISpoofaxAnalyzeUnit analysisUnit, TransformActionContrib action) throws MetaborgException {
        final ITransformConfig config = new TransformConfig(true);
        final ISpoofaxTransformUnit<ISpoofaxAnalyzeUnit> transformUnit =
                S.transformService.transform(analysisUnit, context, action, config);
        
        if (printer != null) {
            for (IMessage message : transformUnit.messages()) {
                printer.print(message, false);
            }
        }
        
        if (!transformUnit.valid()) {
            throw new MetaborgException("Failed to transform " + analysisUnit.source());
        }
        
        final String details = S.strategoCommon.toString(transformUnit.ast());
        return details;
    }
    
    /**
     * Retrieves the transformation action with the given name, for the given language.
     * 
     * @param name
     *      the name of the transformation
     * @param lang
     *      the language
     * 
     * @return
     *      the action
     * 
     * @throws MetaborgException
     *      If the transformation cannot be found, or if multiple transformations match.
     */
    private TransformActionContrib getAction(String name, ILanguageImpl lang) throws MetaborgException {
        final ITransformGoal goal = new EndNamedGoal(name);
        if (!S.actionService.available(lang, goal)) {
            throw new MetaborgException("Cannot find transformation " + name);
        }
        
        final TransformActionContrib action;
        try {
            action = Iterables.getOnlyElement(S.actionService.actionContributions(lang, goal));
        } catch (NoSuchElementException ex) {
            throw new MetaborgException("Transformation " + name + " not a singleton.");
        }
        return action;
    }
}
