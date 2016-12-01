package org.metaborg.meta.nabl2.solver;

import org.spoofax.interpreter.terms.IStrategoTerm;

public interface Message {

    IStrategoTerm getProgramPoint();

    String getMessage();

}