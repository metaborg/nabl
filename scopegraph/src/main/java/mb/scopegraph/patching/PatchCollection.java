package mb.scopegraph.patching;

import java.util.Map;
import java.util.Map.Entry;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

import io.usethesource.capsule.Set;
import mb.scopegraph.oopsla20.diff.BiMap;

public abstract class PatchCollection<S> implements IPatchCollection<S> {

    private static final ILogger logger = LoggerUtils.logger(PatchCollection.class);

    @Override public java.util.Set<Entry<S, S>> allPatches() {
        return Sets.union(patches().entrySet(), new IdentityMappingEntrySet<>(identityPatches()));
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

        @Override public PatchCollection.Immutable<S> putAll(java.util.Set<Entry<S, S>> patches)
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

        private PatchCollection.Immutable<S> putAllUnchecked(java.util.Set<Entry<S, S>> patches)
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
            return new PatchCollection.Immutable<>(patches.freeze(), identityPatches.freeze());
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

        @Override public boolean putAll(java.util.Set<Entry<S, S>> patches) throws InvalidPatchCompositionException {
            boolean changed = false;
            for(Entry<S, S> entry : patches) {
                changed |= put(entry.getKey(), entry.getValue());
            }
            return changed;
        }

        private boolean putAllUnchecked(java.util.Set<Entry<S, S>> patches) throws InvalidPatchCompositionException {
            boolean changed = false;
            for(Entry<S, S> entry : patches) {
                changed |= putUnchecked(entry.getValue(), entry.getKey()); // Entries in map are reversed, so pass value to `oldScope` parameter.
            }
            return changed;
        }

        @Override public boolean putAll(IPatchCollection<S> patches) throws InvalidPatchCompositionException {
            checkInvalidIdentities(patches.identityPatches(), patchDomain());
            checkInvalidIdentities(patches.identityPatches(), patchRange());
            checkInvalidIdentities(identityPatches, patches.patchDomain());
            checkInvalidIdentities(identityPatches, patches.patchRange());

            for(Entry<S, S> patch : patches.patches().entrySet()) {
                final S newScope = patch.getKey();
                final S oldScope = patch.getValue();

                final S otherSrc = this.patches.getKeyOrDefault(newScope, newScope);
                if(!otherSrc.equals(newScope) && !otherSrc.equals(oldScope)) {
                    // Different non-identity patch
                    throwInvalidNewPatch(newScope, oldScope, otherSrc);
                }

                final S otherPatch = this.patches.getValueOrDefault(oldScope, oldScope);
                if(!otherPatch.equals(oldScope) && !otherPatch.equals(newScope)) {
                    // Different non-identity patch
                    throwInvalidOldPatch(newScope, oldScope, otherPatch);
                }
            }

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

    public static <S> void throwInvalidNewPatch(S newScope, S oldScope, S existingOldScope)
            throws InvalidPatchCompositionException {
        throw new InvalidPatchCompositionException("Cannot insert patch " + oldScope + " |-> " + newScope + ". "
                + newScope + " already patched by " + existingOldScope + ".");
    }

    public static <S> void throwInvalidOldPatch(S newScope, S oldScope, S existingNewScope)
            throws InvalidPatchCompositionException {
        throw new InvalidPatchCompositionException("Cannot insert patch " + oldScope + " |-> " + newScope + ". "
                + oldScope + " already patching to " + existingNewScope + ".");
    }

    public static <S> void checkInvalidIdentities(java.util.Set<S> identities, java.util.Set<S> nonIdentities)
            throws InvalidPatchCompositionException {
        final SetView<S> conflicts = Sets.intersection(nonIdentities, identities);
        if(!conflicts.isEmpty()) {
            throw new InvalidPatchCompositionException("Match conflict for " + conflicts + ".");
        }
    }

}
