package org.metaborg.meta.nabl2.solver_new;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.esop.IEsopScopeGraph;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IRelation3;

public interface IIncompleteScopeGraph<S extends IScope & ITerm, L extends ILabel, O extends IOccurrence & ITerm>
        extends IEsopScopeGraph<S, L, O> {

    IRelation3<S, L, ITerm> incompleteDirectEdges();

    IRelation3<S, L, ITerm> incompleteImportEdges();

}