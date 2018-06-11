package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.constraints.Constraints;
import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_debug_constraints extends AnalysisPrimitive {

    public SG_debug_constraints() {
        super(SG_debug_constraints.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return Optional.of(TermSimplifier.focus(unit.resource(), Constraints.build(unit.constraints())));
    }

}