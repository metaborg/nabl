package mb.statix.solver.completeness;

import org.metaborg.util.functions.Predicate3;

import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.EdgeOrData;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IState;

@FunctionalInterface
public interface IsComplete extends Predicate3<Scope, EdgeOrData<ITerm>, IState> {

    static final IsComplete NEVER = (s, l, st) -> false;

    static final IsComplete ALWAYS = (s, l, st) -> true;

}