package mb.scopegraph.pepm16.terms.path;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ConsList;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IResolutionPath;
import mb.scopegraph.pepm16.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class ANStep<S extends IScope, L extends ILabel, O extends IOccurrence> implements IStep<S, L, O> {

    @Value.Parameter @Override public abstract S getSource();

    @Value.Parameter @Override public abstract L getLabel();

    @Value.Parameter public abstract IResolutionPath<S, L, O> getImportPath();

    @Value.Parameter @Override public abstract S getTarget();

    @Value.Lazy @Override public int size() {
        return 1;
    }

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return getImportPath().getImports().__insert(getImportPath().getReference());
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.fromConcat(Iterables2.singleton(getImportPath()), getImportPath().getImportPaths());
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return Set.Immutable.of(getSource(), getTarget());
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return ConsList.of(getLabel());
    }

    @Override public <T> T match(IStep.ICases<S, L, O, T> cases) {
        return cases.caseN(getSource(), getLabel(), getImportPath(), getTarget());
    }

    @Value.Lazy @Override public abstract int hashCode();

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        StringBuilder sb = new StringBuilder();
        if(includeSource) {
            sb.append(getSource());
            sb.append(Paths.PATH_SEPERATOR);
        }
        sb.append("N(");
        sb.append(getLabel());
        sb.append(", ");
        sb.append(getImportPath());
        sb.append(")");
        if(includeTarget) {
            sb.append(Paths.PATH_SEPERATOR);
            sb.append(getTarget());
        }
        return sb.toString();
    }

    @Override public String toString() {
        return toString(true, true);
    }

}