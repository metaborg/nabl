package mb.statix.concurrent.util;

import java.util.HashSet;

import org.metaborg.util.collection.CapsuleUtil;

import com.google.common.collect.Streams;

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

    public Set.Immutable<Entry> get(ITermVar indexVar) {
        return index.get(indexVar);
    }

    public boolean put(V value, Iterable<ITermVar> indexVars, IUnifier unifier) {
        final java.util.Set<ITermVar> vars = new HashSet<>();
        for(ITermVar v : indexVars) {
            vars.addAll(unifier.getVars(v));
        }
        if(vars.isEmpty()) {
            return false;
        }
        final Entry entry = new Entry(value);
        for(ITermVar var : vars) {
            index.__insert(var, entry.inc());
        }
        return true;
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
        final Set<Entry> entries = index.get(indexVar);
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