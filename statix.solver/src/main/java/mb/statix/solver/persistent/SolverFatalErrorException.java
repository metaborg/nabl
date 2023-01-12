package mb.statix.solver.persistent;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.ScopeGraphUtil;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;

public class SolverFatalErrorException extends RuntimeException {

    private static final long serialVersionUID = -42L;

    public SolverFatalErrorException(Throwable cause, IConstraint constraint, IUniDisunifier unifier,
            IScopeGraph<Scope, ITerm, ITerm> scopeGraph, int termDepth) {
        super(formatMessage(constraint, unifier, scopeGraph, termDepth), cause);
    }

    private static String formatMessage(IConstraint constraint,IUniDisunifier unifier, IScopeGraph<Scope, ITerm, ITerm> scopeGraph,
            int termDepth) {
        final StringBuilder sb = new StringBuilder("Fatal error during constraint solving.");
        sb.append("\n* current constraint trace:");
        final TermFormatter formatter = Solver.shallowTermFormatter(unifier, Solver.ERROR_TRACE_TERM_DEPTH);
        MessageUtil.formatTrace(constraint, unifier, formatter, -1).forEach(traceElement -> {
            sb.append("\n\t- ");
            sb.append(traceElement);
        });
        sb.append("\n* current scope graph:\n");
        sb.append(ScopeGraphUtil.toString(scopeGraph, unifier::findRecursive));
        return sb.toString();
    }

}
