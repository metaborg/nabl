package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.metaborg.util.task.NullProgress;
import org.metaborg.util.task.ThreadCancel;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.CriticalEdgeException;
import mb.nabl2.scopegraph.StuckException;
import mb.nabl2.scopegraph.esop.IEsopNameResolution;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.terms.Label;
import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.terms.ITerm;

public class SG_get_decl_resolution extends AnalysisPrimitive {

    public SG_get_decl_resolution() {
        super(SG_get_decl_resolution.class.getSimpleName(), 0);
    }

    @Override public Optional<? extends ITerm> call(ISolution solution, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Occurrence.matcher().match(term, solution.unifier()).<ITerm>flatMap(decl -> {
            IEsopNameResolution<Scope, Label, Occurrence> nameResolution = solution.nameResolution();
            List<ITerm> entries = Lists.newArrayList();
            for(Occurrence ref : solution.scopeGraph().getAllRefs()) {
                try {
                    Collection<IResolutionPath<Scope, Label, Occurrence>> paths =
                            nameResolution.resolve(ref, new ThreadCancel(), new NullProgress());
                    paths.stream().filter(path -> path.getDeclaration().equals(decl))
                            .map(p -> B.newTuple(ref, Paths.toTerm(p))).forEach(entries::add);
                } catch(CriticalEdgeException | StuckException | InterruptedException e) {
                    // ignore
                }
            }
            return Optional.of(B.newList(entries));
        });
    }

}