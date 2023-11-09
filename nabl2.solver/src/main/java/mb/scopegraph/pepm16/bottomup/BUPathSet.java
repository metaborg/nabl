package mb.scopegraph.pepm16.bottomup;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map.Entry;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate2;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.scopegraph.pepm16.ILabel;
import mb.scopegraph.pepm16.IOccurrence;
import mb.scopegraph.pepm16.IScope;
import mb.scopegraph.pepm16.path.IDeclPath;
import mb.scopegraph.pepm16.terms.SpacedName;

abstract class BUPathSet<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    protected abstract SetMultimap<SpacedName, BUPathKey<L>> _keys();

    protected abstract SetMultimap<BUPathKey<L>, P> _paths();


    public boolean isEmpty() {
        return _paths().isEmpty();
    }

    public java.util.Set<SpacedName> names() {
        return _keys().keySet();
    }

    public java.util.Set<BUPathKey<L>> keys(SpacedName name) {
        return _keys().get(name);
    }

    public Collection<P> paths() {
        return _paths().values();
    }

    public Collection<P> paths(SpacedName name) {
        final Set.Transient<P> paths = CapsuleUtil.transientSet();
        for(BUPathKey<L> key : _keys().get(name)) {
            paths.__insertAll(_paths().get(key));
        }
        return paths.freeze();
    }

    public Collection<P> paths(BUPathKey<L> key) {
        return _paths().get(key);
    }


    static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            extends BUPathSet<S, L, O, P> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final SetMultimap.Immutable<SpacedName, BUPathKey<L>> keys;
        private final SetMultimap.Immutable<BUPathKey<L>, P> paths;

        private Immutable(SetMultimap.Immutable<SpacedName, BUPathKey<L>> keys,
                SetMultimap.Immutable<BUPathKey<L>, P> paths) {
            this.keys = keys;
            this.paths = paths;
        }

        @Override protected SetMultimap<SpacedName, BUPathKey<L>> _keys() {
            return keys;
        }

        @Override protected SetMultimap<BUPathKey<L>, P> _paths() {
            return paths;
        }

        public BUPathSet.Immutable<S, L, O, P> filter(Predicate2<BUPathKey<L>, P> filter) {
            BUPathSet.Transient<S, L, O, P> filteredPaths = BUPathSet.Transient.of();
            for(Entry<BUPathKey<L>, P> entry : paths.entrySet()) {
                if(filter.test(entry.getKey(), entry.getValue())) {
                    filteredPaths.add(entry.getKey(), entry.getValue());
                }
            }
            return filteredPaths.freeze();
        }

        public Transient<S, L, O, P> melt() {
            return new Transient<>(keys.asTransient(), paths.asTransient());
        }


        public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
                Immutable<S, L, O, P> of() {
            return new Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            extends BUPathSet<S, L, O, P> {

        private final SetMultimap.Transient<SpacedName, BUPathKey<L>> keys;
        private final SetMultimap.Transient<BUPathKey<L>, P> paths;

        private Transient(SetMultimap.Transient<SpacedName, BUPathKey<L>> keys,
                SetMultimap.Transient<BUPathKey<L>, P> paths) {
            this.keys = keys;
            this.paths = paths;
        }

        @Override protected SetMultimap<SpacedName, BUPathKey<L>> _keys() {
            return keys;
        }

        @Override protected SetMultimap<BUPathKey<L>, P> _paths() {
            return paths;
        }


        public Collection<P> add(BUPathKey<L> key, Collection<P> paths) {
            final Set.Transient<P> added = CapsuleUtil.transientSet();
            keys.__insert(key.name(), key);
            for(P path : paths) {
                if(this.paths.__insert(key, path)) {
                    added.__insert(path);
                }
            }
            return added.freeze();
        }

        public boolean add(BUPathKey<L> key, P path) {
            keys.__insert(key.name(), key);
            return this.paths.__insert(key, path);
        }

        public Collection<P> remove(BUPathKey<L> key) {
            final Set.Immutable<P> removed = this.paths.get(key);
            this.paths.__remove(key);
            keys.__remove(key.name(), key);
            return removed;
        }

        public Collection<P> remove(BUPathKey<L> key, Collection<P> paths) {
            final Set.Transient<P> removed = CapsuleUtil.transientSet();
            for(P path : paths) {
                if(this.paths.__remove(key, path)) {
                    removed.__insert(path);
                }
            }
            if(!this.paths.containsKey(key)) {
                keys.__remove(key.name(), key);
            }
            return removed.freeze();

        }


        public Immutable<S, L, O, P> freeze() {
            return new Immutable<>(keys.freeze(), paths.freeze());
        }


        public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
                Transient<S, L, O, P> of() {
            return new Transient<>(SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }

}