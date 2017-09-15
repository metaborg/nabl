package org.metaborg.meta.nabl2.solver.solvers;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.util.functions.PartialFunction2;

@FunctionalInterface
public interface CallExternal extends PartialFunction2<String, Iterable<? extends ITerm>, ITerm> {

}