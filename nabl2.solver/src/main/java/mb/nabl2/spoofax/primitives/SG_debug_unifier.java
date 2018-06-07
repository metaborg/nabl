package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;
import mb.nabl2.unification.UnifierTerms;

public class SG_debug_unifier extends AnalysisPrimitive {

    public SG_debug_unifier() {
        super(SG_debug_unifier.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(), UnifierTerms.build(sol.unifier()));
        });
    }

}