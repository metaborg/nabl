package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_get_decl_scope extends AnalysisPrimitive {

    public SG_get_decl_scope() {
        super(SG_get_decl_scope.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().<ITerm>flatMap(s -> {
            return Occurrence.matcher().match(term, s.unifier()).<ITerm>flatMap(decl -> {
                return s.scopeGraph().getDecls().get(decl).flatMap(Optional::<ITerm>of);
            });
        });
    }

}