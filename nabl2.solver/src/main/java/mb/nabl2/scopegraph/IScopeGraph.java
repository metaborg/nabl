package mb.nabl2.scopegraph;

import io.usethesource.capsule.Set;
import mb.nabl2.util.collections.IFunction;
import mb.nabl2.util.collections.IRelation3;

public interface IScopeGraph<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set<S> getAllScopes();

    Set<O> getAllDecls();

    Set<O> getAllRefs();

    IFunction<O, S> getDecls();

    IFunction<O, S> getRefs();

    IRelation3<S, L, S> getDirectEdges();

    IRelation3<O, L, S> getExportEdges();

    IRelation3<S, L, O> getImportEdges();

    interface Immutable<S extends IScope, L extends ILabel, O extends IOccurrence> extends IScopeGraph<S, L, O> {

        @Override Set.Immutable<S> getAllScopes();

        @Override Set.Immutable<O> getAllDecls();

        @Override Set.Immutable<O> getAllRefs();

        @Override IFunction.Immutable<O, S> getDecls();

        @Override IFunction.Immutable<O, S> getRefs();

        @Override IRelation3.Immutable<S, L, S> getDirectEdges();

        @Override IRelation3.Immutable<O, L, S> getExportEdges();

        @Override IRelation3.Immutable<S, L, O> getImportEdges();

    }

}