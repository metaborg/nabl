package mb.nabl2.scopegraph.esop.bottomup;

import java.io.Serializable;
import java.util.Collection;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.scopegraph.ILabel;
import mb.nabl2.scopegraph.IOccurrence;
import mb.nabl2.scopegraph.IScope;
import mb.nabl2.scopegraph.path.IDeclPath;
import mb.nabl2.scopegraph.terms.SpacedName;
import mb.nabl2.util.Tuple2;

public abstract class BUPathSet<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>> {

    protected abstract SetMultimap<SpacedName, L> _keys();

    protected abstract SetMultimap<Tuple2<SpacedName, L>, P> _paths();


    public boolean isEmpty() {
        return _paths().isEmpty();
    }

    public java.util.Set<SpacedName> names() {
        return _keys().keySet();
    }

    public java.util.Set<L> labels(SpacedName name) {
        return _keys().get(name);
    }

    public Collection<P> paths() {
        final Set.Transient<P> paths = Set.Transient.of();
        for(Tuple2<SpacedName, L> key : _paths().keySet()) {
            paths.__insertAll(_paths().get(key));
        }
        return paths.freeze();
    }

    public Collection<P> paths(SpacedName name) {
        final Set.Transient<P> paths = Set.Transient.of();
        for(L label : _keys().get(name)) {
            paths.__insertAll(_paths().get(Tuple2.of(name, label)));
        }
        return paths.freeze();
    }

    public Collection<P> paths(SpacedName name, L label) {
        return _paths().get(Tuple2.of(name, label));
    }


    public static class Immutable<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            extends BUPathSet<S, L, O, P> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final SetMultimap.Immutable<SpacedName, L> keys;
        private final SetMultimap.Immutable<Tuple2<SpacedName, L>, P> paths;

        private Immutable(SetMultimap.Immutable<SpacedName, L> keys,
                SetMultimap.Immutable<Tuple2<SpacedName, L>, P> paths) {
            this.keys = keys;
            this.paths = paths;
        }

        @Override protected SetMultimap<SpacedName, L> _keys() {
            return keys;
        }

        @Override protected SetMultimap<Tuple2<SpacedName, L>, P> _paths() {
            return paths;
        }


        public Transient<S, L, O, P> melt() {
            return new Transient<>(keys.asTransient(), paths.asTransient());
        }


        public static <S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
                Immutable<S, L, O, P> of() {
            return new Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }

    public static class Transient<S extends IScope, L extends ILabel, O extends IOccurrence, P extends IDeclPath<S, L, O>>
            extends BUPathSet<S, L, O, P> {

        private final SetMultimap.Transient<SpacedName, L> keys;
        private final SetMultimap.Transient<Tuple2<SpacedName, L>, P> paths;

        private Transient(SetMultimap.Transient<SpacedName, L> keys,
                SetMultimap.Transient<Tuple2<SpacedName, L>, P> paths) {
            this.keys = keys;
            this.paths = paths;
        }

        @Override protected SetMultimap<SpacedName, L> _keys() {
            return keys;
        }

        @Override protected SetMultimap<Tuple2<SpacedName, L>, P> _paths() {
            return paths;
        }


        public Collection<P> add(SpacedName name, L label, Collection<P> paths) {
            final Set.Transient<P> added = Set.Transient.of();
            keys.__insert(name, label);
            final Tuple2<SpacedName, L> key = Tuple2.of(name, label);
            for(P path : paths) {
                if(this.paths.__insert(key, path)) {
                    added.__insert(path);
                }
            }
            return added.freeze();
        }

        public boolean add(SpacedName name, L label, P path) {
            keys.__insert(name, label);
            return this.paths.__insert(Tuple2.of(name, label), path);
        }

        public Collection<P> remove(SpacedName name, L label) {
            final Tuple2<SpacedName, L> key = Tuple2.of(name, label);
            final Set.Immutable<P> removed = this.paths.get(key);
            this.paths.__remove(key);
            keys.__remove(name, label);
            return removed;
        }

        public Collection<P> remove(SpacedName name, L label, Collection<P> paths) {
            final Set.Transient<P> removed = Set.Transient.of();
            final Tuple2<SpacedName, L> key = Tuple2.of(name, label);
            for(P path : paths) {
                if(this.paths.__remove(key, path)) {
                    removed.__insert(path);
                }
            }
            if(!this.paths.containsKey(key)) {
                keys.__remove(name, label);
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