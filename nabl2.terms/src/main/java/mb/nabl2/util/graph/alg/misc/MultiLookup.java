package mb.nabl2.util.graph.alg.misc;

import com.google.common.collect.Maps;

import mb.nabl2.util.collections.MultiSetMap;
import mb.nabl2.util.graph.alg.misc.memory.IMemoryView;
import mb.nabl2.util.graph.alg.misc.memory.MapBackedMemoryView;

public class MultiLookup<Key, Value> implements IMultiLookup<Key, Value> {

    private final MultiSetMap.Transient<Key, Value> data;

    public MultiLookup() {
        this.data = MultiSetMap.Transient.of();
    }

    @Override public IMemoryView<Value> lookup(Key key) {
        if(!data.containsKey(key)) {
            return null;
        }
        return new MapBackedMemoryView<>(Maps.newHashMap(data.get(key).toMap()));
    }

    @Override public Iterable<Key> distinctKeys() {
        return data.keySet();
    }

    @Override public int countKeys() {
        return data.keySet().size();
    }

    @Override public ChangeGranularity addPair(Key key, Value value) {
        final ChangeGranularity change;
        if(!data.containsKey(key)) {
            change = ChangeGranularity.KEY;
        } else if(!data.contains(key, value)) {
            change = ChangeGranularity.VALUE;
        } else {
            change = ChangeGranularity.DUPLICATE;
        }
        data.put(key, value);
        return change;
    }

    @Override public ChangeGranularity addPairOrNop(Key key, Value value) {
        return addPair(key, value);
    }

    @Override public ChangeGranularity removePair(Key key, Value value) {
        if(!data.contains(key, value)) {
            throw new IllegalStateException();
        }
        data.remove(key, value);
        final ChangeGranularity change;
        if(!data.containsKey(key)) {
            change = ChangeGranularity.KEY;
        } else if(!data.contains(key, value)) {
            change = ChangeGranularity.VALUE;
        } else {
            change = ChangeGranularity.DUPLICATE;
        }
        return change;
    }

    @Override public ChangeGranularity addPairPositiveMultiplicity(Key key, Value value, int count) {
        final ChangeGranularity change;
        if(!data.containsKey(key)) {
            change = ChangeGranularity.KEY;
        } else if(!data.contains(key, value)) {
            change = ChangeGranularity.VALUE;
        } else {
            change = ChangeGranularity.DUPLICATE;
        }
        data.put(key, value, count);
        return change;
    }

    @Override public void clear() {
        data.clear();
    }

}