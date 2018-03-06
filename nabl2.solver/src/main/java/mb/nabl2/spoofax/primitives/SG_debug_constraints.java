package mb.nabl2.spoofax.primitives;

import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_debug_constraints extends AnalysisNoTermPrimitive {

    public SG_debug_constraints() {
        super(SG_debug_constraints.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return Optional.of(TermSimplifier.focus(unit.resource(), Constraints.build(unit.constraints())));
    }

}