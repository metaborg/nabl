package mb.nabl2.scopegraph.terms.path;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IScopePath;
import mb.nabl2.util.collections.ConsList;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class DeclPath<S extends IScope, L extends ILabel, O extends IOccurrence> implements IDeclPath<S, L, O> {

    @Value.Parameter @Override public abstract IScopePath<S, L, O> getPath();

    @Value.Parameter @Override public abstract O getDeclaration();

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return getPath().getImports();
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return getPath().getScopes();
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return getPath().getLabels();
    }

    @Value.Lazy @Override public abstract int hashCode();

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