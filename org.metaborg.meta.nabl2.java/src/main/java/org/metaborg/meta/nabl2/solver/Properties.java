package org.metaborg.meta.nabl2.solver;

import java.util.Map;
import java.util.Optional;

import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.terms.ITerm;

import com.google.common.collect.Maps;

public class Properties implements IProperties<Occurrence> {

    private final Map<Occurrence,Map<ITerm,ITerm>> data;

    public Properties() {
        this.data = Maps.newHashMap();
    }

    @Override public Iterable<Occurrence> getAllDecls() {
        return data.keySet();
    }

    @Override public Iterable<ITerm> getDefinedKeys(Occurrence occurrence) {
        return occurrenceData(occurrence).keySet();
    }

    @Override public Optional<ITerm> getValue(Occurrence occurrence, ITerm key) {
        return Optional.ofNullable(occurrenceData(occurrence).get(key));
    }

    public Optional<ITerm> putValue(Occurrence occurrence, ITerm key, ITerm value) {
        return Optional.ofNullable(occurrenceData(occurrence).put(key, value));
    }

    private Map<ITerm,ITerm> occurrenceData(Occurrence occurrence) {
        return data.computeIfAbsent(occurrence, o -> Maps.newHashMap());
    }

}