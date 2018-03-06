package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.Scope;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_get_scope_refs extends AnalysisPrimitive {

    public SG_get_scope_refs() {
        super(SG_get_scope_refs.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().<ITerm>flatMap(s -> {
            return Scope.matcher().match(term, s.unifier()).<ITerm>map(scope -> {
                return B.newList(s.scopeGraph().getRefs().inverse().get(scope));
            });
        });
    }

}