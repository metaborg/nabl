package mb.statix.concurrent.util;

import java.util.Collection;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.u.IUnifier;

public class VarIndexedCollection<V> {

    private final SetMultimap.Transient<ITermVar, Entry> index;

    public VarIndexedCollection() {
        this.index = SetMultimap.Transient.of();
    }

    public boolean put(V value, Collection<ITermVar> indexVars, IUnifier unifier) {
        final Set<ITermVar> vars =
                indexVars.stream().flatMap(v -> unifier.getVars(v).stream()).collect(CapsuleCollectors.toSet());
        final Entry entry = new Entry(value);
        for(ITermVar var : vars) {
            index.__insert(var, entry.inc());
        }
        return !vars.isEmpty();
    }

    public Set.Immutable<V> update(Iterable<ITermVar> indexVars, IUnifier unifier) {
        final Set.Transient<V> done = CapsuleUtil.transientSet();
        for(ITermVar indexVar : indexVars) {
            update(indexVar, unifier, done);
        }
        return done.freeze();
    }

    public Set.Immutable<V> update(ITermVar indexVar, IUnifier unifier) {
        final Set.Transient<V> done = CapsuleUtil.transientSet();
        update(indexVar, unifier, done);
        return done.freeze();
    }

    private void update(ITermVar indexVar, IUnifier unifier, Set.Transient<V> done) {
        final Set<ITermVar> vars = unifier.getVars(indexVar);
        final Iterable<Entry> entries = index.get(indexVar);
        index.__remove(indexVar);
        for(Entry entry : entries) {
            entry.dec();
            for(ITermVar var : vars) {
                index.__insert(var, entry.inc());
            }
            if(entry.refcount() == 0) {
                done.__insert(entry.value);
            }
        }
    }

    public class Entry {
        private final V value;
        private int refcount;

        public Entry(V value) {
            this.value = value;
            this.refcount = 0;
        }

        public V value() {
            return value;
        }

        public int refcount() {
            return refcount;
        }

        public Entry dec() {
            refcount -= 1;
            return this;
        }

        public Entry inc() {
            refcount += 1;
            return this;
        }

    }

}
