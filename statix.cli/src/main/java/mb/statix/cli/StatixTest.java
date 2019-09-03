package mb.statix.cli;

import java.util.Optional;

import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class StatixTest {

    private final Statix STX;

    public StatixTest(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException {
        final TransformActionContrib evalAction = STX.getAction("Evaluate Test", STX.lang);
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = STX.loadFile(file);
        if(!maybeAnalysisUnit.isPresent()) {
            return;
        }
        final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
        final IStrategoTerm ast = analysisUnit.ast();
        if(ast != null && Tools.isTermAppl(ast) && Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
            STX.messagePrinter.print(new Message("Evaluating test.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
            final String typing = STX.format(STX.transform(analysisUnit, evalAction));
            STX.msgStream.println(typing);
        }

    }

}