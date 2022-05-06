package mb.scopegraph.patching;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.metaborg.util.RefBool;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class PatchCollection<S> implements IPatchCollection<S> {

    private static final ILogger logger = LoggerUtils.logger(PatchCollection.class);

    private volatile int hashCode = 0; // lazily computed.

    @Override public java.util.Set<Entry<S, S>> allPatches() {
        return Sets.union(patches().entrySet(), new IdentityMappingEntrySet<>(identityPatches()));
    }

    @SuppressWarnings("unchecked") @Override public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }
        if(obj == null) {
            return false;
        }
        if(!obj.getClass().equals(this.getClass())) {
            return false;
        }

        final PatchCollection<S> other = (PatchCollection<S>) obj;

        return Objects.equals(identityPatches(), other.identityPatches()) && Objects.equals(patches(), other.patches());
    }

    @Override public int hashCode() {
        int result = hashCode;
        if(result == -1) {
            result = 11;
            result += 31 * identityPatches().hashCode();
            result += 37 * patches().hashCode();
            hashCode = result;
        }
        return result;
    }

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        final RefBool first = new RefBool(true);

        patches().asMap().forEach((newScope, oldScope) -> {
            if(!first.get()) {
                sb.append(", ");
            }
            sb.append(newScope);
            sb.append(" <-| ");
            sb.append(oldScope);
            first.set(false);
        });

        identityPatches().forEach(scope -> {
            if(!first.get()) {
                sb.append(", ");
            }
            sb.append(scope);
            sb.append(" <-| ");
            sb.append(scope);
            first.set(false);
        });

        sb.append("}");
        return sb.toString();
    }

    public void assertConsistent() throws InvalidPatchCompositionException {
        checkInvalidIdentities(identityPatches(), patchDomain());
        checkInvalidIdentities(identityPatches(), patchRange());
    }

    public static class Immutable<S> extends PatchCollection<S> implements IPatchCollection.Immutable<S> {

        @SuppressWarnings({ "rawtypes", "unchecked" }) private static final PatchCollection.Immutable EMPTY =
                new PatchCollection.Immutable(BiMap.Immutable.of(), Set.Immutable.of());

        /**
         * Maps new scopes to old scopes.
         */
        private final BiMap.Immutable<S> patches;

        /**
         * Set of scopes that are explicitly mapping to themselves.
         */
        private final Set.Immutable<S> identityPatches;

        public Immutable(BiMap.Immutable<S> patches, Set.Immutable<S> identityPatches) {
            this.patches = patches;
            this.identityPatches = identityPatches;
        }

        @Override public boolean isEmpty() {
            return patches.isEmpty() && identityPatches.isEmpty();
        }

        @Override public boolean isIdentity() {
            return patches.isEmpty();
        }

        @Override public boolean isIdentity(S oldScope) {
            return !patches.containsValue(oldScope);
        }

        @Override public S patch(S oldScope) {
            if(isIdentity()) {
                return oldScope;
            }

            final S result = patches.getValue(oldScope);
            if(result != null) {
                return result;
            }
            final S otherSrc = patches.getKey(oldScope);
            if(otherSrc != null) {
                // Invariant: oldScope not in identity matches.
                logger.warn(
                        "Suspicious patch application. Applying implicit identity match to {}, but that scope is also matched to by {}.",
                        oldScope, otherSrc);
            }
            return oldScope;
        }

        @Override public PatchCollection.Transient<S> melt() {
            return new PatchCollection.Transient<>(patches.melt(), identityPatches.asTransient());
        }

        @Override public BiMap.Immutable<S> patches() {
            return patches;
        }

        @Override public java.util.Set<S> identityPatches() {
            return identityPatches;
        }

        @Override public PatchCollection.Immutable<S> put(S newScope, S oldScope)
                throws InvalidPatchCompositionException {
            final PatchCollection.Transient<S> _patches = melt();
            if(!_patches.put(newScope, oldScope)) {
                return this;
            }

            return _patches.freeze();
        }

        @Override public PatchCollection.Immutable<S> putAll(Map<S, S> patches)
                throws InvalidPatchCompositionException {
            return putAll(patches.entrySet());
        }

        @Override public PatchCollection.Immutable<S> putAll(Collection<? extends Entry<S, S>> patches)
                throws InvalidPatchCompositionException {
            if(patches.isEmpty()) {
                return this;
            }

            final PatchCollection.Transient<S> _patches = melt();
            if(!_patches.putAll(patches)) {
                return this;
            }

            return _patches.freeze();
        }

        private PatchCollection.Immutable<S> putAllUnchecked(Collection<Entry<S, S>> patches)
                throws InvalidPatchCompositionException {
            if(patches.isEmpty()) {
                return this;
            }

            final PatchCollection.Transient<S> _patches = melt();
            if(!_patches.putAllUnchecked(patches)) {
                return this;
            }

            return _patches.freeze();
        }

        @Override public PatchCollection.Immutable<S> putAll(IPatchCollection<S> patches)
                throws InvalidPatchCompositionException {
            if(patches.isEmpty()) {
                return this;
            }

            final PatchCollection.Transient<S> _patches = melt();
            if(!_patches.putAll(patches)) {
                return this;
            }

            return _patches.freeze();
        }

        @Override public java.util.Set<S> patchDomain() {
            return patches.valueSet();
        }

        @Override public java.util.Set<S> patchRange() {
            return patches.keySet();
        }

        @SuppressWarnings("unchecked") public static <S> PatchCollection.Immutable<S> of() {
            return EMPTY;
        }

        @SuppressWarnings("unchecked") public static <S> PatchCollection.Immutable<S> of(BiMap<S> patches) {
            // Don't just set the patches property, because the argument can contain identity patches
            return EMPTY.putAllUnchecked(patches.entrySet());
        }

    }

    public static class Transient<S> extends PatchCollection<S> implements IPatchCollection.Transient<S> {

        private BiMap.Transient<S> patches;

        private final Set.Transient<S> identityPatches;

        public Transient(BiMap.Transient<S> patches, Set.Transient<S> identityPatches) {
            this.identityPatches = identityPatches;
            this.patches = patches;
        }

        @Override public boolean isEmpty() {
            return patches.isEmpty() && identityPatches.isEmpty();
        }

        @Override public boolean isIdentity() {
            return patches.isEmpty();
        }

        @Override public boolean isIdentity(S oldScope) {
            return !patches.containsValue(oldScope);
        }

        @Override public S patch(S oldScope) {
            if(isIdentity()) {
                return oldScope;
            }

            final S result = patches.getValue(oldScope);
            if(result != null) {
                return result;
            }
            final S otherSrc = patches.getKey(oldScope);
            if(otherSrc != null) {
                // Invariant: oldScope not in identity matches.
                logger.warn(
                        "Suspicious patch application. Applying implicit identity match to {}, but that scope is also matched to by {}.",
                        oldScope, otherSrc);
            }
            return oldScope;
        }

        @Override public PatchCollection.Immutable<S> freeze() {
            return new PatchCollection.Immutable<>(patches(), identityPatches.freeze());
        }

        @Override public BiMap.Immutable<S> patches() {
            final BiMap.Immutable<S> currentPatches = patches.freeze();
            patches = currentPatches.melt();
            return currentPatches;
        }

        @Override public java.util.Set<S> identityPatches() {
            return identityPatches;
        }

        @Override public boolean put(S newScope, S oldScope) throws InvalidPatchCompositionException {
            if(patches.containsEntry(newScope, oldScope)) {
                // Invariant: newScope != oldScope
                return false;
            }

            if(patches.containsKey(newScope)) {
                throwInvalidNewPatch(newScope, oldScope, patches.getKey(newScope));
            }

            if(patches.containsValue(oldScope)) {
                throwInvalidOldPatch(newScope, oldScope, patches.getValue(oldScope));
            }

            if(oldScope.equals(newScope)) {
                return identityPatches.__insert(newScope);
            } else {
                if(identityPatches.contains(newScope)) {
                    throwInvalidNewPatch(newScope, oldScope, identityPatches.get(newScope));
                }

                if(identityPatches.contains(oldScope)) {
                    throwInvalidOldPatch(newScope, oldScope, identityPatches.get(oldScope));
                }

                return patches.put(newScope, oldScope);
            }
        }

        private boolean putUnchecked(S oldScope, S newScope) {
            if(oldScope.equals(newScope)) {
                return identityPatches.__insert(oldScope);
            } else {
                return patches.put(newScope, oldScope);
            }
        }

        @Override public boolean putAll(Map<S, S> patches) throws InvalidPatchCompositionException {
            return putAll(patches.entrySet());
        }

        @Override public boolean putAll(Collection<? extends Entry<S, S>> patches)
                throws InvalidPatchCompositionException {
            boolean changed = false;
            for(Entry<S, S> entry : patches) {
                changed |= put(entry.getKey(), entry.getValue());
            }
            return changed;
        }

        private boolean putAllUnchecked(Collection<Entry<S, S>> patches) throws InvalidPatchCompositionException {
            boolean changed = false;
            for(Entry<S, S> entry : patches) {
                changed |= putUnchecked(entry.getValue(), entry.getKey()); // Entries in map are reversed, so pass value to `oldScope` parameter.
            }
            return changed;
        }

        @Override public boolean putAll(IPatchCollection<S> patches) throws InvalidPatchCompositionException {
            boolean changed = identityPatches.__insertAll(patches.identityPatches());
            changed |= this.patches.putAll(patches.patches().entrySet());
            return changed;
        }

        @Override public java.util.Set<S> patchDomain() {
            return patches.valueSet();
        }

        @Override public java.util.Set<S> patchRange() {
            return patches.keySet();
        }

        public static <S> PatchCollection.Transient<S> of() {
            return new PatchCollection.Transient<>(BiMap.Transient.of(), Set.Transient.of());
        }

    }

    private static <S> void throwInvalidNewPatch(S newScope, S oldScope, S existingOldScope)
            throws InvalidPatchCompositionException {
        throw new InvalidPatchCompositionException("Cannot insert patch " + oldScope + " |-> " + newScope + ". "
                + newScope + " already patched by " + existingOldScope + ".");
    }

    private static <S> void throwInvalidOldPatch(S newScope, S oldScope, S existingNewScope)
            throws InvalidPatchCompositionException {
        throw new InvalidPatchCompositionException("Cannot insert patch " + oldScope + " |-> " + newScope + ". "
                + oldScope + " already patching to " + existingNewScope + ".");
    }

    private static <S> void checkInvalidIdentities(java.util.Set<S> identities, java.util.Set<S> nonIdentities)
            throws InvalidPatchCompositionException {
        final SetView<S> conflicts = Sets.intersection(nonIdentities, identities);
        if(!conflicts.isEmpty()) {
            throw new InvalidPatchCompositionException("Match conflict for " + conflicts + ".");
        }
    }

}
