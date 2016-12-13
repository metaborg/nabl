package org.metaborg.meta.nabl2.solver;

import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.terms.ITerm;

public interface IProperties<O extends IOccurrence> {

    Iterable<O> getAllDecls();

    Iterable<ITerm> getDefinedKeys(O decl);

    Optional<ITerm> getValue(O decl, ITerm key);

}