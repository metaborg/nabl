package mb.scopegraph.pepm16.terms.path;

import jakarta.annotation.Nullable;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ConsList;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IScopePath;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AResolutionPath<S extends IScope, L extends ILabel, O extends IOccurrence>
        implements IResolutionPath<S, L, O> {

    @Value.Parameter @Override public abstract O getReference();

    @Value.Parameter @Override public abstract IScopePath<S, L, O> getPath();

    @Value.Parameter @Override public abstract O getDeclaration();

    @Value.Check public @Nullable AResolutionPath<S, L, O> check() {
        if(!IOccurrence.match(getReference(), getDeclaration())) {
            return null;
        }
        if(getPath().getImports().contains(getReference())) {
            return null;
        }
        return this;
    }

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return getPath().getImports();
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return getPath().getScopes();
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return getPath().getLabels();
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return getPath().getImportPaths();
    }

    @Value.Lazy @Override public abstract int hashCode();

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getReference());
        sb.append(Paths.PATH_SEPERATOR);
        sb.append("R");
        sb.append(Paths.PATH_SEPERATOR);
        sb.append(getPath());
        sb.append(Paths.PATH_SEPERATOR);
        sb.append("D");
        sb.append(Paths.PATH_SEPERATOR);
        sb.append(getDeclaration());
        return sb.toString();
    }

}