package mb.nabl2.util.graph.alg.misc;

import mb.nabl2.util.collections.MultiSetMap;
import mb.nabl2.util.graph.alg.misc.memory.IMemoryView;
import mb.nabl2.util.graph.alg.misc.memory.MapBackedMemoryView;

public class MultiLookup<Key, Value> implements IMultiLookup<Key, Value> {

    private MultiSetMap.Immutable<Key, Value> data;

    public MultiLookup() {
        this.data = MultiSetMap.Immutable.of();
    }

    @Override public IMemoryView<Value> lookup(Key key) {
        if(!data.containsKey(key)) {
            return null;
        }
        return new MapBackedMemoryView<>(data.get(key).asMap());
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
        data = data.put(key, value);
        return change;
    }

    @Override public ChangeGranularity addPairOrNop(Key key, Value value) {
        return addPair(key, value);
    }

    @Override public ChangeGranularity removePair(Key key, Value value) {
        if(!data.contains(key, value)) {
            throw new IllegalStateException();
        }
        data = data.remove(key, value);
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
        data = data.put(key, value, count);
        return change;
    }

    @Override public void clear() {
        data = MultiSetMap.Immutable.of();
    }

}