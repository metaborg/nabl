package mb.statix.cli;

import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class StatixTest {

    private static final ILogger log = LoggerUtils.logger(StatixTest.class);

    private final Statix STX;

    public StatixTest(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException {
        final FileObject resource = STX.S.resolve(file);
        final TransformActionContrib evalAction = STX.cli.getNamedTransformAction("Evaluate Test", STX.stxLang);
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = STX.loadStxFile(resource);
        if(!maybeAnalysisUnit.isPresent()) {
            return;
        }
        final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
        final IStrategoTerm ast = analysisUnit.ast();
        if(ast != null && Tools.isTermAppl(ast) && Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
            log.info("Evaluating test.");
            final String typing = STX.format(STX.cli.transform(analysisUnit, evalAction, STX.context));
            STX.msgStream.println(typing);
        }

    }

}