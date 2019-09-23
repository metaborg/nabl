package mb.statix.cli;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.TransformActionContrib;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;

import com.google.common.collect.Iterables;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.Constraints;
import mb.statix.random.RandomTermGenerator;
import mb.statix.random.SearchNode;
import mb.statix.random.SearchState;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class StatixGenerate {

    private static final ILogger log = LoggerUtils.logger(StatixGenerate.class);

    private static final boolean DEBUG = false;

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
                final ILanguageComponent lc = Iterables.getOnlyElement(pl.components());
                HybridInterpreter runtime;
                try {
                    runtime = STX.S.strategoRuntimeService.runtime(lc, STX.context, false);
                } catch(MetaborgException e) {
                    throw new MetaborgRuntimeException(e);
                }
                return (t) -> {
                    final IStrategoTerm st = strategoTerms.toStratego(explicate(t));
                    try {
                        final IStrategoTerm r = STX.S.strategoCommon.invoke(runtime, st, "pp-generated");
                        return r != null ? Tools.asJavaString(r) : t.toString();
                    } catch(MetaborgException e) {
                        return t.toString();
                    }
                };
            }).orElseGet(() -> ITerm::toString);

            log.info("Generating random terms.");
            final RandomTermGenerator rtg = new RandomTermGenerator(spec, constraint, Paret.allIn());
            int hits = 0;
            while(true) {
                final SearchNode<SearchState> state;
                if((state = rtg.next().orElse(null)) == null) {
                    break;
                }
                hits++;
                printResult("SUCCESS", state, Level.Info, Level.Debug, pp);
            }
            log.info("Generated {} random terms.", hits);
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

    private void printResult(String header, SearchNode<SearchState> node, Level level1, Level level2,
            Function1<ITerm, String> pp) {
        if(DEBUG) {
            log.log(level1, "+--- {} ---+.", header);

            node.output().print(s -> log.log(level1, s), (t, u) -> pp.apply(u.findRecursive(t)));

            log.log(level1, "+~~~ Trace ~~~+.");

            boolean first = true;
            SearchNode<?> traceNode = node;
            do {
                log.log(first ? level1 : level2, "+ * {}", traceNode);
                first = false;
                if(traceNode.output() instanceof SearchState) {
                    SearchState state = (SearchState) traceNode.output();
                    IUnifier u = state.state().unifier();
                    log.log(level2, "+   constraints: {}",
                            Constraints.toString(state.constraints(), t -> pp.apply(u.findRecursive(t))));
                }
            } while((traceNode = traceNode.parent()) != null);

            log.log(level1, "+------------+");
        } else {
            final Map<ITermVar, ITermVar> ex = node.output().existentials();
            final IState.Immutable state = node.output().state();
            final ITerm t = ex.get(B.newVar("", "e"));
            final String p = pp.apply(state.unifier().findRecursive(t));
            System.out.println(p);
        }
    }

}