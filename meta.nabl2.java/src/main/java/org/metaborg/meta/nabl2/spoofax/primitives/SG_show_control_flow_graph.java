package org.metaborg.meta.nabl2.spoofax.primitives;

import static org.metaborg.meta.nabl2.terms.build.TermBuild.B;

import java.util.Optional;

import org.metaborg.meta.nabl2.controlflow.terms.ControlFlowGraphTerms;
import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphUnit;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_show_control_flow_graph extends AnalysisNoTermPrimitive {

    public SG_show_control_flow_graph() {
        super(SG_show_control_flow_graph.class.getSimpleName());
    }

    @Override public Optional<? extends ITerm> call(IScopeGraphUnit unit) throws InterpreterException {
        return unit.solution().filter(sol -> unit.isPrimary()).map(sol -> {
            return B.newString(ControlFlowGraphTerms.toDot(sol.flowSpecSolution()));
        });
    }

}
