package org.metaborg.meta.nabl2.scopegraph;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;

public interface INameResolution<S extends IScope, L extends ILabel, O extends IOccurrence> {

    Set<S> getAllScopes();

    Set<O> getAllRefs();

    Set<IResolutionPath<S, L, O>> resolve(O ref);

    Set<IDeclPath<S, L, O>> visible(S scope);

    Set<IDeclPath<S, L, O>> reachable(S scope);

    static <S extends IScope, L extends ILabel, O extends IOccurrence> INameResolution<S, L, O> empty() {
        return new EmptyNameResolution<>();
    }

    static class EmptyNameResolution<S extends IScope, L extends ILabel, O extends IOccurrence>
            implements INameResolution<S, L, O>, Serializable {

        private static final long serialVersionUID = 42L;

        @Override public Set<S> getAllScopes() {
            return Collections.emptySet();
        }

        @Override public Set<O> getAllRefs() {
            return Collections.emptySet();
        }

        @Override public Set<IResolutionPath<S, L, O>> resolve(O ref) {
            return Collections.emptySet();
        }

        @Override public Set<IDeclPath<S, L, O>> visible(S scope) {
            return Collections.emptySet();
        }

        @Override public Set<IDeclPath<S, L, O>> reachable(S scope) {
            return Collections.emptySet();
        }

        @Override public int hashCode() {
            return 0;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            return true;
        }
    }

}