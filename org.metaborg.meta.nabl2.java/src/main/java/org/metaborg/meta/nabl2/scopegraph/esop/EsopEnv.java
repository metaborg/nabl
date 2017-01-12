package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IPath;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableSpacedName;
import org.metaborg.meta.nabl2.scopegraph.terms.SpacedName;
import org.metaborg.meta.nabl2.util.functions.Function1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class EsopEnv<S extends IScope, L extends ILabel, O extends IOccurrence> implements Serializable {

    private static final long serialVersionUID = 42L;

    private boolean complete;
    private Multimap<SpacedName,IPath<S,L,O>> res;

    private EsopEnv(boolean complete) {
        this.complete = complete;
        this.res = HashMultimap.create();
    }

    public boolean isComplete() {
        return complete;
    }

    void add(IPath<S,L,O> path) {
        O decl = path.getDeclaration();
        res.put(ImmutableSpacedName.of(decl.getNamespace(), decl.getName()), path);
    }

    public Iterable<IPath<S,L,O>> get(O ref) {
        return res.get(ImmutableSpacedName.of(ref.getNamespace(), ref.getName()));
    }

    public Iterable<IPath<S,L,O>> getAll() {
        return res.values();
    }

    public void map(Function1<IPath<S,L,O>,IPath<S,L,O>> f) {
        for (SpacedName sn : res.keySet()) {
            res.replaceValues(sn, res.get(sn).stream().map(f::apply).collect(Collectors.toSet()));
        }
    }

    public void shadow(EsopEnv<S,L,O> other) {
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

    public void union(EsopEnv<S,L,O> other) {
        complete &= other.complete;
        for (SpacedName sn : other.res.keySet()) {
            res.putAll(sn, other.res.get(sn));
        }
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopEnv<S,L,O> empty(boolean complete) {
        return new EsopEnv<>(complete);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopEnv<S,L,O> of(boolean complete,
            Iterable<IPath<S,L,O>> paths) {
        EsopEnv<S,L,O> env = new EsopEnv<>(complete);
        for (IPath<S,L,O> path : paths) {
            env.add(path);
        }
        return env;
    }

}