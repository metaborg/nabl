package mb.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.scopegraph.terms.ScopeGraphTerms;
import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.spoofax.analysis.IScopeGraphUnit;
import mb.nabl2.terms.ITerm;

public class SG_debug_scope_graph extends AnalysisPrimitive {

    public SG_debug_scope_graph() {
        super(SG_debug_scope_graph.class.getSimpleName());
    }

    @Override protected Optional<? extends ITerm> call(IScopeGraphUnit unit, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return TermSimplifier.focus(unit.resource(),
                    ScopeGraphTerms.build(sol.scopeGraph(), sol.declProperties(), sol.unifier()));
        });
    }

}