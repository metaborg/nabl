package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Maps;

public class Properties<T> implements IProperties<T>, Serializable {

    private static final long serialVersionUID = 42L;

    private final Map<T,Map<ITerm,ITerm>> data;

    public Properties() {
        this.data = Maps.newHashMap();
    }

    @Override public Set<T> getIndices() {
        return data.keySet();
    }

    @Override public Set<ITerm> getDefinedKeys(T index) {
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

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + data.hashCode();
        return result;
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked") final Properties<T> other = (Properties<T>) obj;
        if(!data.equals(other.data))
            return false;
        return true;
    }

}