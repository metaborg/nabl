package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

public class SG_analysis_has_errors extends AnalysisPrimitive {

    public SG_analysis_has_errors() {
        super(SG_analysis_has_errors.class.getSimpleName());
    }

    @Override public Optional<? extends IStrategoTerm> call(IScopeGraphUnit unit, IStrategoTerm sterm,
            List<IStrategoTerm> sterms, ITermFactory factory) throws InterpreterException {
        return unit.solution().flatMap(s -> {
            if(s.messages().getErrors().isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(sterm);
            }
        });
    }

}