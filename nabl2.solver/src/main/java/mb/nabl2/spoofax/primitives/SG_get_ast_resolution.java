package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.scopegraph.terms.path.Paths;
import mb.nabl2.solver.ISolution;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;

public class SG_get_ast_resolution extends AnalysisPrimitive {

    public SG_get_ast_resolution() {
        super(SG_get_ast_resolution.class.getSimpleName(), 0);
    }

    @SuppressWarnings("unlikely-arg-type") @Override public Optional<? extends ITerm> call(ISolution solution,
            ITerm term, List<ITerm> terms) throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            List<ITerm> entries = Lists.newArrayList();
            for(Occurrence ref : solution.scopeGraph().getAllRefs()) {
                if(ref.getIndex().equals(index)) {
                    solution.nameResolution().resolve(ref).map(Paths::resolutionPathsToDecls).ifPresent(decls -> {
                        decls.stream().forEach(decl -> {
                            entries.add(B.newTuple(ref, decl.getName()));
                        });
                    });
                }
            }
            if(entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(B.newList(entries));
        });
    }

}