package org.metaborg.meta.nabl2.unification;

import java.util.Map;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public final class UnificationResult {

    private final Set<ITermVar> substituted;
    private final SetMultimap<ITerm, ITerm> residual;

    UnificationResult() {
        this.substituted = Sets.newHashSet();
        this.residual = HashMultimap.create();
    }

    boolean addResidual(ITerm left, ITerm right) {
        return residual.put(left, right);
    }

    boolean addSubstituted(ITermVar var) {
        return substituted.add(var);
    }

    public Set<ITermVar> getSubstituted() {
        return substituted;
    }

    public Set<Map.Entry<ITerm, ITerm>> getResidual() {
        return residual.entries();
    }

}