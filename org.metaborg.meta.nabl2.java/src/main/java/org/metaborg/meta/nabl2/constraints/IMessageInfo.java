package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.unification.IUnifier;

public interface IMessageInfo {

    UnsatisfiableException makeException(String defaultMessage, Iterable<ITerm> programPoints, IUnifier unifier);

}