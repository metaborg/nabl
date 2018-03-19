package mb.nabl2.solver.solvers;

import org.metaborg.util.functions.PartialFunction2;

import mb.nabl2.terms.ITerm;

@FunctionalInterface
public interface CallExternal extends PartialFunction2<String, Iterable<? extends ITerm>, ITerm> {

}