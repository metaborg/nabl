package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;

import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.solver.ISolution;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;

public class SG_get_ast_decls extends AnalysisPrimitive {

    public SG_get_ast_decls() {
        super(SG_get_ast_decls.class.getSimpleName());
    }

    @SuppressWarnings("unlikely-arg-type") @Override public Optional<ITerm> call(ISolution solution, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        return TermIndex.get(term).flatMap(index -> {
            List<ITerm> entries = Lists.newArrayList();
            for(Occurrence decl : solution.scopeGraph().getAllDecls()) {
                if(decl.getIndex().equals(index)) {
                    entries.add(decl);
                }
            }
            return Optional.of(B.newList(entries));
        });
    }

}