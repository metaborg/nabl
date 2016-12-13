package org.metaborg.meta.nabl2.solver;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Maps;

public class Properties<T> implements IProperties<T> {

    private final Map<T,Map<ITerm,ITerm>> data;

    public Properties() {
        this.data = Maps.newHashMap();
    }

    @Override public Iterable<T> getIndices() {
        return data.keySet();
    }

    @Override public Iterable<ITerm> getDefinedKeys(T index) {
        return indexData(index).keySet();
    }

    @Override public Optional<ITerm> getValue(T index, ITerm key) {
        return Optional.ofNullable(indexData(index).get(key));
    }

    public Optional<ITerm> putValue(T index, ITerm key, ITerm value) {
        return Optional.ofNullable(indexData(index).put(key, value));
    }

    private Map<ITerm,ITerm> indexData(T index) {
        return data.computeIfAbsent(index, o -> Maps.newHashMap());
    }

}