package mb.nabl2.spoofax.primitives;

import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.symbolic.SymbolicTerms;
import mb.nabl2.terms.ITerm;

public class SG_debug_symbolic_constraints extends AnalysisNoTermPrimitive {

    public SG_debug_symbolic_constraints() {
        super(SG_debug_symbolic_constraints.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(), SymbolicTerms.build(sol.symbolic(), sol.unifier()));
        });
    }

}