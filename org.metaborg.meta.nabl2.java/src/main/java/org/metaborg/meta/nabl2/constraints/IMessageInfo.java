package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.solver.UnsatisfiableException;
import org.metaborg.meta.nabl2.terms.ITerm;

public interface IMessageInfo {

    UnsatisfiableException makeException(String defaultMessage, Iterable<ITerm> programPoints);

}