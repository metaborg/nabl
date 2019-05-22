package mb.nabl2.scopegraph.terms.path;

import java.util.Iterator;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Iterators;

import io.usethesource.capsule.Set;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IResolutionPath;
import mb.nabl2.scopegraph.path.IStep;
import mb.nabl2.util.collections.PSequence;

@Value.Immutable
@Serial.Version(value = 42L)
abstract class EStep<S extends IScope, L extends ILabel, O extends IOccurrence> implements IStep<S, L, O> {

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

    @Value.Lazy @Override public PSequence<L> getLabels() {
        return PSequence.of(getLabel());
    }

    @Override public Iterator<IStep<S, L, O>> iterator() {
        return Iterators.singletonIterator(this);
    }

    @Override public <T> T match(IStep.ICases<S, L, O, T> cases) {
        return cases.caseE(getSource(), getLabel(), getTarget());
    }

    @Override public String toString(boolean includeTo, boolean includeFrom) {
        StringBuilder sb = new StringBuilder();
        if(includeFrom) {
            sb.append(getSource());
            sb.append(Paths.PATH_SEPERATOR);
        }
        sb.append("E(");
        sb.append(getLabel());
        sb.append(")");
        if(includeTo) {
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