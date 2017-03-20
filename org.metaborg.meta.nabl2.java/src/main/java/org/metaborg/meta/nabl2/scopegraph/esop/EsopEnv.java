package org.metaborg.meta.nabl2.scopegraph.esop;

import java.io.Serializable;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.scopegraph.ILabel;
import org.metaborg.meta.nabl2.scopegraph.IOccurrence;
import org.metaborg.meta.nabl2.scopegraph.IScope;
import org.metaborg.meta.nabl2.scopegraph.path.IDeclPath;
import org.metaborg.meta.nabl2.scopegraph.path.IResolutionPath;
import org.metaborg.meta.nabl2.scopegraph.terms.ImmutableSpacedName;
import org.metaborg.meta.nabl2.scopegraph.terms.SpacedName;
import org.metaborg.meta.nabl2.scopegraph.terms.path.Paths;
import org.metaborg.meta.nabl2.util.Optionals;
import org.metaborg.meta.nabl2.util.functions.Function1;
import org.metaborg.meta.nabl2.util.functions.PartialFunction1;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class EsopEnv<S extends IScope, L extends ILabel, O extends IOccurrence> implements Serializable {

    private static final long serialVersionUID = 42L;

    private boolean complete;
    private Multimap<SpacedName, IDeclPath<S, L, O>> res;

    private EsopEnv(boolean complete) {
        this.complete = complete;
        this.res = HashMultimap.create();
    }

    public boolean isComplete() {
        return complete;
    }

    void add(IDeclPath<S, L, O> path) {
        O decl = path.getDeclaration();
        res.put(ImmutableSpacedName.of(decl.getNamespace(), decl.getName()), path);
    }

    public Iterable<IResolutionPath<S, L, O>> get(O ref) {
        return res.get(ImmutableSpacedName.of(ref.getNamespace(), ref.getName())).stream()
            .map(p -> Paths.resolve(ref, p)).flatMap(Optionals::stream).collect(Collectors.toList());
    }

    public Iterable<IDeclPath<S, L, O>> getAll() {
        return res.values();
    }

    public void map(Function1<IDeclPath<S, L, O>, IDeclPath<S, L, O>> f) {
        for(SpacedName sn : res.keySet()) {
            res.replaceValues(sn, res.get(sn).stream().map(f::apply).collect(Collectors.toSet()));
        }
    }

    public void filter(PartialFunction1<IDeclPath<S, L, O>, IDeclPath<S, L, O>> f) {
        for(SpacedName sn : res.keySet()) {
            res.replaceValues(sn,
                res.get(sn).stream().map(f::apply).flatMap(Optionals::stream).collect(Collectors.toSet()));
        }
    }

    public void shadow(EsopEnv<S, L, O> other) {
        if(!complete) {
            return;
        }
        complete &= other.complete;
        for(SpacedName sn : other.res.keySet()) {
            if(!res.containsKey(sn)) {
                res.putAll(sn, other.res.get(sn));
            }
        }
    }

    public void union(EsopEnv<S, L, O> other) {
        complete &= other.complete;
        for(SpacedName sn : other.res.keySet()) {
            res.putAll(sn, other.res.get(sn));
        }
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopEnv<S, L, O> empty(boolean complete) {
        return new EsopEnv<>(complete);
    }

    public static <S extends IScope, L extends ILabel, O extends IOccurrence> EsopEnv<S, L, O> of(boolean complete,
        Iterable<IDeclPath<S, L, O>> paths) {
        EsopEnv<S, L, O> env = new EsopEnv<>(complete);
        for(IDeclPath<S, L, O> path : paths) {
            env.add(path);
        }
        return env;
    }

}