package mb.statix.taico.util;

import java.io.Serializable;

import io.usethesource.capsule.SetMultimap;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.nabl2.util.collections.IRelation3;

public abstract class LightWeightHashTrieRelation3<K, L, V> implements IRelation3<K, L, V> {

    protected LightWeightHashTrieRelation3() {
    }

    protected abstract SetMultimap<K, Tuple2<L, V>> fwdK();

    protected abstract SetMultimap<Tuple2<K, L>, V> fwdKL();

    @Override public SetMultimap<Tuple2<K, L>, V> _getForwardMap() {
        return fwdKL();
    }

    @Override public boolean contains(K key) {
        return fwdK().containsKey(key);
    }

    @Override public boolean contains(K key, L label) {
        return fwdKL().containsKey(ImmutableTuple2.of(key, label));
    }

    @Override public boolean contains(K key, L label, V value) {
        return fwdK().containsEntry(key, ImmutableTuple2.of(label, value));
    }

    @Override public java.util.Set<K> keySet() {
        return fwdK().keySet();
    }

    @Override public java.util.Set<V> valueSet() {
        throw new UnsupportedOperationException();
    }

    @Override public java.util.Set<V> get(K key, L label) {
        return fwdKL().get(ImmutableTuple2.of(key, label));
    }

    @Override public java.util.Set<Tuple2<L, V>> get(K key) {
        return fwdK().get(key);
    }

    @Override public boolean isEmpty() {
        return fwdK().isEmpty();
    }

    @Override public String toString() {
        return fwdK().toString();
    }


    public static class Immutable<K, L, V> extends LightWeightHashTrieRelation3<K, L, V>
            implements IRelation3.Immutable<K, L, V>, Serializable {
        private static final long serialVersionUID = 42L;

        private final SetMultimap.Immutable<K, Tuple2<L, V>> fwdK;
        private final SetMultimap.Immutable<Tuple2<K, L>, V> fwdKL;

        Immutable(SetMultimap.Immutable<K, Tuple2<L, V>> fwdK, SetMultimap.Immutable<Tuple2<K, L>, V> fwdKL) {
            this.fwdK = fwdK;
            this.fwdKL = fwdKL;
        }

        @Override protected SetMultimap<K, Tuple2<L, V>> fwdK() {
            return fwdK;
        }

        @Override protected SetMultimap<Tuple2<K, L>, V> fwdKL() {
            return fwdKL;
        }

        @Override public IRelation3.Immutable<K, L, V> put(K key, L label, V value) {
            return new LightWeightHashTrieRelation3.Immutable<>(fwdK.__insert(key, ImmutableTuple2.of(label, value)),
                    fwdKL.__insert(ImmutableTuple2.of(key, label), value));
        }

        @Override public IRelation3.Immutable<K, L, V> putAll(IRelation3<K, L, V> other) {
            final IRelation3.Transient<K, L, V> that = melt();
            that.putAll(other);
            return that.freeze();
        }

        @Override public IRelation3.Immutable<V, L, K> inverse() {
            throw new UnsupportedOperationException();
        }

        @Override public LightWeightHashTrieRelation3.Transient<K, L, V> melt() {
            return new LightWeightHashTrieRelation3.Transient<>(fwdK.asTransient(), fwdKL.asTransient());
        }

        @Override public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + fwdK.hashCode();
            result = prime * result + fwdKL.hashCode();
            return result;
        }

        @Override public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked") final LightWeightHashTrieRelation3.Immutable<K, L, V> other =
                    (LightWeightHashTrieRelation3.Immutable<K, L, V>) obj;
            if(!fwdK.equals(other.fwdK))
                return false;
            if(!fwdKL.equals(other.fwdKL))
                return false;
            return true;
        }

        public static <K, L, V> LightWeightHashTrieRelation3.Immutable<K, L, V> of() {
            return new LightWeightHashTrieRelation3.Immutable<>(SetMultimap.Immutable.of(), SetMultimap.Immutable.of());
        }

    }


    public static class Transient<K, L, V> extends LightWeightHashTrieRelation3<K, L, V> implements IRelation3.Transient<K, L, V> {

        private final SetMultimap.Transient<K, Tuple2<L, V>> fwdK;
        private final SetMultimap.Transient<Tuple2<K, L>, V> fwdKL;

        Transient(SetMultimap.Transient<K, Tuple2<L, V>> fwdK, SetMultimap.Transient<Tuple2<K, L>, V> fwdKL) {
            this.fwdK = fwdK;
            this.fwdKL = fwdKL;
        }

        @Override protected SetMultimap<K, Tuple2<L, V>> fwdK() {
            return fwdK;
        }

        @Override protected SetMultimap<Tuple2<K, L>, V> fwdKL() {
            return fwdKL;
        }

        @Override public boolean put(K key, L label, V value) {
            if(fwdK.__insert(key, ImmutableTuple2.of(label, value))) {
                fwdKL.__insert(ImmutableTuple2.of(key, label), value);
                return true;

            }
            return false;
        }

        @Override public boolean putAll(IRelation3<K, L, V> other) {
            return other.stream().reduce(false,
                    (change, klv) -> Boolean.logicalOr(change, put(klv._1(), klv._2(), klv._3())), Boolean::logicalOr);
        }

        @Override public boolean remove(K key) {
            java.util.Set<Tuple2<L, V>> entries;
            if(!(entries = fwdK.get(key)).isEmpty()) {
                fwdK.__remove(key);
                for(Tuple2<L, V> entry : entries) {
                    L label = entry._1();
                    fwdKL.__remove(ImmutableTuple2.of(key, label));
                }
                return true;
            }
            return false;
        }

        @Override public boolean remove(K key, L label) {
            return fwdKL.__remove(ImmutableTuple2.of(key, label));
        }

        @Override public boolean remove(K key, L label, V value) {
            if(fwdK.__remove(key, ImmutableTuple2.of(label, value))) {
                fwdKL.__remove(ImmutableTuple2.of(key, label), value);
                return true;
            }
            return false;
        }

        @Override public IRelation3.Transient<V, L, K> inverse() {
            throw new UnsupportedOperationException();
        }

        @Override public LightWeightHashTrieRelation3.Immutable<K, L, V> freeze() {
            return new LightWeightHashTrieRelation3.Immutable<>(fwdK.freeze(), fwdKL.freeze());
        }

        public static <K, L, V> LightWeightHashTrieRelation3.Transient<K, L, V> of() {
            return new LightWeightHashTrieRelation3.Transient<>(SetMultimap.Transient.of(), SetMultimap.Transient.of());
        }

    }
}