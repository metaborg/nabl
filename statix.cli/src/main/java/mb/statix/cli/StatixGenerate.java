package mb.statix.cli;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.messages.Message;
import org.metaborg.core.messages.MessageSeverity;
import org.metaborg.core.messages.MessageType;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.random.RandomTermGenerator;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private final Statix STX;

    public StatixGenerate(Statix stx) {
        this.STX = stx;
    }

    public void run(String file) throws MetaborgException, InterruptedException {
        final FileObject resource = STX.S.resolve(file);
        final TransformActionContrib evalAction = STX.getAction("Evaluation Pair", STX.stxLang);
        final Optional<ISpoofaxAnalyzeUnit> maybeAnalysisUnit = STX.loadStxFile(resource);
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
                    new StrategoTerms(STX.S.termFactoryService.get(STX.stxLang, STX.project, false));
            final IConstraint constraint =
                    StatixTerms.constraint().match(strategoTerms.fromStratego(evalPair.getSubterm(0)))
                            .orElseThrow(() -> new MetaborgException("Expected constraint"));
            final Spec spec = StatixTerms.spec().match(strategoTerms.fromStratego(evalPair.getSubterm(1)))
                    .orElseThrow(() -> new MetaborgException("Expected spec"));

            Function1<ITerm, String> pp;
            pp = STX.findProject(resource).flatMap(localProject -> {
                return STX.loadProjectLang(localProject);
            }).<Function1<ITerm, String>>map(pl -> {
                return (t) -> {
                    final IStrategoTerm st = strategoTerms.toStratego(explicate(t));
                    try {
                        final IStrategoTerm r = STX.S.strategoCommon.invoke(pl, STX.context, st, "pp-generated");
                        return r != null ? Tools.asJavaString(r) : t.toString();
                    } catch(MetaborgException e) {
                        return t.toString();
                    }
                };
            }).orElseGet(() -> ITerm::toString);

            STX.messagePrinter.print(new Message("Generating random terms.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
            final RandomTermGenerator rtg = new RandomTermGenerator(spec, constraint, pp);
            while(true) {
                Optional<SearchState> state = rtg.next();
                if(!state.isPresent()) {
                    break;
                }
            }
            STX.messagePrinter.print(new Message("Generated random terms.", MessageSeverity.NOTE, MessageType.INTERNAL,
                    analysisUnit.source(), null, null), false);
        }

    }

    private static ITerm explicate(ITerm t) {
        // @formatter:off
        return T.sometd(
            M.<ITerm>cases(
                M.cons(M.term(), M.<ITerm>var(StatixGenerate::explicate), (cons, hd, tl) -> B.newCons(explicate(hd), B.newList(explicate(tl)), cons.getAttachments())),
                M.var(v -> B.newAppl("Var", B.newString(v.getResource()), B.newString(v.getName())))
            )::match
        ).apply(t);
        // @formatter:on
    }

}