package mb.scopegraph.patching;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import mb.scopegraph.oopsla20.diff.BiMap;

public interface IPatchCollection<S> {

    /**
     * @return {@code true} when there are no patches in this patch set, {@code false} otherwise.
     */
    boolean isEmpty();

    /**
     * @return {@code true} when {@code s |-> s} for all {@code s}, {@code false} otherwise.
     *
     *         Differs from {@link #isEmpty()} in the sense that the patch set can contain explicit identity patches,
     *         which cannot be composed with other patches for these scopes.
     */
    boolean isIdentity();

    boolean isIdentity(S oldScope);

    S patch(S oldScope);

    /**
     * @return Patch collection including identity patches.
     */
    Set<Map.Entry<S, S>> allPatches();

    /**
     * @return Patch collection excluding identity patches.
     */
    BiMap.Immutable<S> patches();

    Set<S> identityPatches();

    /**
     * @return Domain of non-identity patches.
     */
    Set<S> patchDomain();

    /**
     * @return Range of non-identity patches.
     */
    Set<S> patchRange();

    void assertConsistent() throws InvalidPatchCompositionException;

    interface Immutable<S> extends IPatchCollection<S> {

        Transient<S> melt();

        Immutable<S> put(S newScope, S oldScope) throws InvalidPatchCompositionException;

        Immutable<S> putAll(Map<S, S> patches) throws InvalidPatchCompositionException;

        Immutable<S> putAll(Collection<? extends Map.Entry<S, S>> patches) throws InvalidPatchCompositionException;

        Immutable<S> putAll(IPatchCollection<S> patches) throws InvalidPatchCompositionException;

    }

    interface Transient<S> extends IPatchCollection<S> {

        Immutable<S> freeze();

        boolean put(S newScope, S oldScope) throws InvalidPatchCompositionException;

        boolean putAll(Map<S, S> patches) throws InvalidPatchCompositionException;

        boolean putAll(Collection<? extends Map.Entry<S, S>> patches) throws InvalidPatchCompositionException;

        boolean putAll(IPatchCollection<S> patches) throws InvalidPatchCompositionException;

    }

}
