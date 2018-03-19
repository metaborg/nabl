package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.Occurrence;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_get_decl_property extends AnalysisPrimitive {

    public SG_get_decl_property() {
        super(SG_get_decl_property.class.getSimpleName(), 1);
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        if(terms.size() != 1) {
            throw new InterpreterException("Need one term argument: key");
        }
        ITerm key = terms.get(0);
        return unit.solution().<ITerm>flatMap(s -> {
            return Occurrence.matcher().match(term, s.unifier()).<ITerm>flatMap(decl -> {
                return s.declProperties().getValue(decl, key).map(s.unifier()::findRecursive);
            });
        });
    }

}