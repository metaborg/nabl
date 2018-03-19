package mb.nabl2.spoofax.primitives;

import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.NameResolutionTerms;
import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_debug_name_resolution extends AnalysisNoTermPrimitive {

    public SG_debug_name_resolution() {
        super(SG_debug_name_resolution.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(),
                    NameResolutionTerms.build(sol.scopeGraph(), sol.nameResolution()));
        });
    }

}