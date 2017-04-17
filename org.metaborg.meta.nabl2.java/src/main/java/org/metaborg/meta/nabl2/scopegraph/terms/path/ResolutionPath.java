package org.metaborg.meta.nabl2.scopegraph.terms.path;

import javax.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.path.IScopePath;
import org.pcollections.PSequence;
import org.pcollections.PSet;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ResolutionPath<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IResolutionPath<S, L, O> {

    @Value.Parameter @Override public abstract O getReference();

    @Value.Parameter @Override public abstract IScopePath<S, L, O> getPath();

    @Value.Parameter @Override public abstract O getDeclaration();

    @Value.Check public @Nullable ResolutionPath<S, L, O> check() {
        if(!IOccurrence.match(getReference(), getDeclaration())) {
            return null;
        }
        if(getPath().getImports().contains(getReference())) {
            return null;
        }
        return this;
    }

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
        return getPath().getImportPaths();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getReference());
        sb.append("R");
        sb.append(Paths.PATH_SEPERATOR);
        sb.append(getPath());
        sb.append(Paths.PATH_SEPERATOR);
        sb.append("D");
        sb.append(getDeclaration());
        return sb.toString();
    }

}