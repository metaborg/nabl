package org.metaborg.meta.nabl2.unification;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.util.iterators.Iterables2;

import io.usethesource.capsule.Map;

public abstract class Substitution implements ISubstitution {

    protected abstract Map<ITermVar, ITerm> entries();

    public Set<ITermVar> keySet() {
        return entries().keySet();
    }

    public boolean contains(ITermVar var) {
        return entries().containsKey(var);
    }

    public Optional<ITerm> get(ITermVar var) {
        return Optional.ofNullable(entries().get(var));
    }

    public boolean isEmpty() {
        return entries().isEmpty();
    }

    public ITerm find(ITerm term) {
        return term.isGround() ? term
                : M.somebu(M.preserveLocking(t -> M.var().match(t).flatMap(this::get))).apply(term);
    }

    public static class Immutable extends Substitution implements ISubstitution.Immutable {

        private final Map.Immutable<ITermVar, ITerm> entries;

        private Immutable(io.usethesource.capsule.Map.Immutable<ITermVar, ITerm> entries) {
            this.entries = entries;
        }

        @Override protected Map<ITermVar, ITerm> entries() {
            return entries;
        }

        @Override public Substitution.Immutable put(ITermVar var, ITerm term) {
            return new Substitution.Immutable(entries.__put(var, term));
        }

        @Override public Substitution.Immutable putAll(java.util.Map<ITermVar, ITerm> otherEntries) {
            return new Substitution.Immutable(entries.__putAll(otherEntries));
        }

        @Override public Substitution.Immutable remove(ITermVar var) {
            return new Substitution.Immutable(entries.__remove(var));
        }

        @Override public Substitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final Map.Transient<ITermVar, ITerm> newEntries = entries.asTransient();
            Iterables2.stream(vars).forEach(newEntries::__remove);
            return new Substitution.Immutable(newEntries.freeze());
        }

        public ISubstitution.Transient melt() {
            return new Substitution.Transient(entries.asTransient());
        }

        public static ISubstitution.Immutable of() {
            return new Substitution.Immutable(Map.Immutable.of());
        }

    }

    public static class Transient extends Substitution implements ISubstitution.Transient {

        private final Map.Transient<ITermVar, ITerm> entries;

        private Transient(Map.Transient<ITermVar, ITerm> entries) {
            this.entries = entries;
        }

        @Override protected Map<ITermVar, ITerm> entries() {
            return entries;
        }

        public boolean put(ITermVar var, ITerm term) {
            return !Objects.equals(entries.__put(var, term), term);
        }

        public boolean putAll(java.util.Map<ITermVar, ITerm> otherEntries) {
            return entries.__putAll(otherEntries);
        }

        public boolean remove(ITermVar var) {
            return entries.__remove(var) != null;
        }

        public boolean removeAll(Iterable<ITermVar> vars) {
            boolean change = false;
            for(ITermVar var : vars) {
                change |= remove(var);
            }
            return change;
        }

        public ISubstitution.Immutable freeze() {
            return new Substitution.Immutable(entries.freeze());
        }

        public static ISubstitution.Transient of() {
            return new Substitution.Transient(Map.Transient.of());
        }

    }

}