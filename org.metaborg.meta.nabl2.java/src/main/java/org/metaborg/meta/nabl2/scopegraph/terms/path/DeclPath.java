package org.metaborg.meta.nabl2.scopegraph.terms.path;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.metaborg.util.iterators.Iterables2;
import org.pcollections.PSequence;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class DeclPath<S extends IScope, L extends ILabel, O extends IOccurrence> implements IDeclPath<S, L, O> {

    @Value.Parameter @Override public abstract IScopePath<S, L, O> getPath();

    @Value.Parameter @Override public abstract O getDeclaration();

    @Value.Lazy @Override public PSet<O> getImports() {
        return getPath().getImports();
    }

    @Value.Lazy @Override public PSet<S> getScopes() {
        return getPath().getScopes();
    }

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return getPath().getLabels();
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.empty();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPath());
        sb.append(Paths.PATH_SEPERATOR);
        sb.append("D");
        sb.append(getDeclaration());
        return sb.toString();
    }

}