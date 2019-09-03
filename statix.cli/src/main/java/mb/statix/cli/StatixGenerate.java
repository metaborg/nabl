package mb.statix.cli;

import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.random.RandomTermGenerator;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class StatixGenerate {

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException {
        final TransformActionContrib evalAction = STX.getAction("Evaluation Pair", STX.lang);
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = STX.loadFile(file);
        if(!maybeAnalysisUnit.isPresent()) {
            return;
        }
        final ISpoofaxAnalyzeUnit analysisUnit = maybeAnalysisUnit.get();
        final IStrategoTerm ast = analysisUnit.ast();
        if(ast != null && Tools.isTermAppl(ast) && Tools.hasConstructor((IStrategoAppl) analysisUnit.ast(), "Test")) {
            final IStrategoTerm evalPair = STX.transform(analysisUnit, evalAction);
            if(!Tools.isTermTuple(evalPair) || evalPair.getSubtermCount() != 2) {
                throw new MetaborgException("Expected tuple of constraint and spec, but got " + evalPair);
            }
            final StrategoTerms strategoTerms =
                    new StrategoTerms(STX.S.termFactoryService.get(STX.lang, STX.project, false));
            final IConstraint constraint =
                    StatixTerms.constraint().match(strategoTerms.fromStratego(evalPair.getSubterm(0)))
                            .orElseThrow(() -> new MetaborgException("Expected constraint"));
            final Spec spec = StatixTerms.spec().match(strategoTerms.fromStratego(evalPair.getSubterm(1)))
                    .orElseThrow(() -> new MetaborgException("Expected spec"));
            STX.messagePrinter.print(new Message("Generating random terms.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
            final RandomTermGenerator rtg = new RandomTermGenerator(spec, constraint);
            while(true) {
                Optional<SearchState> state = rtg.next();
                if(!state.isPresent()) {
                    break;
                }
                printResult(state.get(), analysisUnit.source());
            }
            STX.messagePrinter.print(new Message("Generated random terms.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
        }

    }

    private void printResult(SearchState state, FileObject source) {
        STX.messagePrinter.print(
                new Message("+-- Result --+.", MessageSeverity.NOTE, MessageType.INTERNAL, source, null, null), false);
        final IUnifier unifier = state.state().unifier();
        for(Entry<ITermVar, ITermVar> existential : state.existentials().entrySet()) {
            final String entry = existential.getKey() + ": " + unifier.toString(existential.getValue());
            STX.messagePrinter.print(new Message(entry, MessageSeverity.NOTE, MessageType.INTERNAL, source, null, null),
                    false);
        }
        STX.messagePrinter.print(
                new Message("+------------+", MessageSeverity.NOTE, MessageType.INTERNAL, source, null, null), false);
    }

}