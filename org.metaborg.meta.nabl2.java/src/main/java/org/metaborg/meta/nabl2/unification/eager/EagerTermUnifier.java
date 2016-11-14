package org.metaborg.meta.nabl2.unification.eager;

import java.util.Set;

import org.metaborg.meta.nabl2.collections.Throws;
import org.metaborg.meta.nabl2.collections.fastutil.Object2ObjectOpenHashPMap;
import org.metaborg.meta.nabl2.collections.fastutil.Object2ObjectPMap;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFunction;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.unification.ITermUnifier;
import org.metaborg.meta.nabl2.unification.UnificationException;

public final class EagerTermUnifier implements ITermUnifier {

    final Object2ObjectPMap<ITermVar,ITerm> varReps;
    final Object2ObjectPMap<ITerm,ITerm> termReps;

    public EagerTermUnifier() {
        this.varReps = new Object2ObjectOpenHashPMap<>();
        this.termReps = new Object2ObjectOpenHashPMap<>();
    }

    EagerTermUnifier(Object2ObjectPMap<ITermVar,ITerm> varReps, Object2ObjectPMap<ITerm,ITerm> termReps) {
        this.varReps = varReps;
        this.termReps = termReps;
    }

    @Override public Throws<EagerUnifyResult,UnificationException> unify(ITerm term1, ITerm term2) {
        final EagerFindResult result1 = find(term1);
        final EagerFindResult result2 = result1.unifier().find(term2);
        EagerTermUnifier unifier = result2.unifier();
        return result1.rep().apply(ITermFunction.of(appl -> {
            return null;
        }, tuple -> {
            return null;
        }, cons -> {
            return null;
        }, nil -> {
            return null;
        }, string -> {
            return null;
        }, integer -> {
            return null;
        }, op -> {
            return null;
        }, var -> {
            return null;
        }));
    }

    @Override public Throws<EagerFindResult,UnificationException> find(ITerm term) {
        return find(term).flatMap(result -> {
            final EagerTermUnifier unifier = result.unifier();
            return result.rep().apply(ITermFunction.of(appl -> {
                return null;
            }, tuple -> {
                return null;
            }, cons -> {
                return null;
            }, nil -> {
                return null;
            }, string -> {
                return null;
            }, integer -> {
                return null;
            }, op -> {
                return null;
            }, var -> {
                if (unifier.varReps.containsKey(var)) {
                    // final EagerFindResult result2 =
                    // find(unifier.varReps.get(var));
                    // return new EagerFindResult(result2.rep(), new
                    // EagerTermUnifier(
                    // result2.unifier().varReps.put(var, result2.rep()),
                    // result2.unifier().termReps));
                } else {
                    // return new EagerFindResult(var, unifier);
                }
                return null;
            }));
        });
    }

    @Override public Set<ITermVar> variables() {
        return varReps.keySet();
    }

}