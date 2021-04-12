package mb.nabl2.scopegraph.terms.path;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.collection.ConsList;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IStep;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class AEStep<S extends IScope, L extends ILabel, O extends IOccurrence> implements IStep<S, L, O> {

    @Value.Parameter @Override public abstract S getSource();

    @Value.Parameter @Override public abstract L getLabel();

    @Value.Parameter @Override public abstract S getTarget();

    @Value.Lazy @Override public int size() {
        return 1;
    }

    @Value.Lazy @Override public Set.Immutable<O> getImports() {
        return Set.Immutable.of();
    }

    @Override public Iterable<IResolutionPath<S, L, O>> getImportPaths() {
        return Iterables2.empty();
    }

    @Value.Lazy @Override public Set.Immutable<S> getScopes() {
        return Set.Immutable.of(getSource(), getTarget());
    }

    @Value.Lazy @Override public ConsList<L> getLabels() {
        return ConsList.of(getLabel());
    }

    @Override public <T> T match(IStep.ICases<S, L, O, T> cases) {
        return cases.caseE(getSource(), getLabel(), getTarget());
    }

    @Override public String toString(boolean includeSource, boolean includeTarget) {
        StringBuilder sb = new StringBuilder();
        if(includeSource) {
            sb.append(getSource());
            sb.append(Paths.PATH_SEPERATOR);
        }
        sb.append("E(");
        sb.append(getLabel());
        sb.append(")");
        if(includeTarget) {
            sb.append(Paths.PATH_SEPERATOR);
            sb.append(getTarget());
        }
        return sb.toString();
    }

    @Value.Lazy @Override public abstract int hashCode();

    @Override public String toString() {
        return toString(true, true);
    }

}