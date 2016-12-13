package org.metaborg.meta.nabl2.scopegraph.esop;

import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableSpacedName;
import org.metaborg.meta.nabl2.scopegraph.terms.SpacedName;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class EsopEnv<O extends IOccurrence> {

    private boolean complete;
    private Multimap<SpacedName,O> res;

    private EsopEnv(boolean complete) {
        this.complete = complete;
        this.res = HashMultimap.create();
    }

    public boolean isComplete() {
        return complete;
    }

    void add(O decl) {
        res.put(ImmutableSpacedName.of(decl.getNamespace(), decl.getName()), decl);
    }

    public Iterable<O> get(O ref) {
        return res.get(ImmutableSpacedName.of(ref.getNamespace(), ref.getName()));
    }

    public void shadow(EsopEnv<O> other) {
        if (!complete) {
            return;
        }
        complete &= other.complete;
        for (SpacedName sn : other.res.keySet()) {
            if (!res.containsKey(sn)) {
                res.putAll(sn, other.res.get(sn));
            }
        }
    }

    public void union(EsopEnv<O> other) {
        complete &= other.complete;
        for (SpacedName sn : other.res.keySet()) {
            res.putAll(sn, other.res.get(sn));
        }
    }

    public static <O extends IOccurrence> EsopEnv<O> empty(boolean complete) {
        return new EsopEnv<>(complete);
    }

    public static <O extends IOccurrence> EsopEnv<O> of(boolean complete, Iterable<O> decls) {
        EsopEnv<O> env = new EsopEnv<>(complete);
        for (O decl : decls) {
            env.add(decl);
        }
        return env;
    }

}