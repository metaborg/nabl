package mb.nabl2.terms.unification.ud;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.TermSize;
import mb.nabl2.terms.unification.u.IUnifier;

public abstract class BaseUniDisunifier implements IUniDisunifier, Serializable {

    private static final long serialVersionUID = 42L;

    protected abstract IUnifier.Immutable unifier();

    @Override public Map.Immutable<ITermVar, ITerm> equalityMap() {
        return unifier().equalityMap();
    }

    ///////////////////////////////////////////
    // unifier functions
    ///////////////////////////////////////////

    @Override public boolean isEmpty() {
        return unifier().isEmpty() && disequalities().isEmpty();
    }

    @Override public boolean contains(ITermVar var) {
        // disequalities do not contribute to the domain
        return unifier().contains(var);
    }

    @Override public java.util.Set<ITermVar> varSet() {
        // disequalities do not contribute to the domain
        // as a consequence, this.isEmpty() != this.varSet().isEmpty()
        return unifier().varSet();
    }

    @Override public java.util.Set<ITermVar> freeVarSet() {
        // FIXME Include disequalities: disequalities.freeVars - unifier.vars?
        return unifier().freeVarSet();
    }

    @Override public boolean isCyclic() {
        // FIXME Include disequalities
        return unifier().isCyclic();
    }

    ///////////////////////////////////////////
    // equals
    ///////////////////////////////////////////

    @Override public boolean equals(Object other) {
        if(other == null) {
            return false;
        }
        if(other == this) {
            return true;
        }
        if(!(other instanceof IUniDisunifier)) {
            return false;
        }
        final IUniDisunifier that = (IUniDisunifier) other;
        return equals(that);
    }

    public boolean equals(IUniDisunifier other) {
        // FIXME Include disequalities
        return unifier().equals(other);
    }

    @Override public int hashCode() {
        return Objects.hash(unifier(), disequalities());
    }

    ///////////////////////////////////////////
    // toString
    ///////////////////////////////////////////

    @Override public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append(unifier());
        sb.append(";");
        boolean first = true;
        for(Diseq disequality : disequalities()) {
            sb.append(first ? " " : ", ");
            first = false;
            sb.append(disequality);
        }
        sb.append(first ? "}" : " }");
        return sb.toString();
    }

    ///////////////////////////////////////////
    // findTerm(ITerm) / findRep(ITerm)
    ///////////////////////////////////////////

    @Override public boolean hasTerm(ITermVar var) {
        return unifier().hasTerm(var);
    }

    @Override public ITerm findTerm(ITerm term) {
        return unifier().findTerm(term);
    }

    ///////////////////////////////////////////
    // findRecursive(ITerm)
    ///////////////////////////////////////////

    @Override public ITerm findRecursive(final ITerm term) {
        return unifier().findRecursive(term);
    }

    ///////////////////////////////////////////
    // isCyclic(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isCyclic(final ITerm term) {
        return unifier().isCyclic(term);
    }

    ///////////////////////////////////////////
    // isGround(ITerm)
    ///////////////////////////////////////////

    @Override public boolean isGround(final ITerm term) {
        return unifier().isGround(term);
    }

    ///////////////////////////////////////////
    // getVars(ITerm)
    ///////////////////////////////////////////

    @Override public Set.Immutable<ITermVar> getVars(final ITerm term) {
        return unifier().getVars(term);
    }

    ///////////////////////////////////////////
    // size(ITerm)
    ///////////////////////////////////////////

    @Override public TermSize size(final ITerm term) {
        return unifier().size(term);
    }

    ///////////////////////////////////////////
    // toString(ITerm)
    ///////////////////////////////////////////

    @Override public String toString(final ITerm term) {
        return unifier().toString(term);
    }

    @Override public String toString(final ITerm term, int n) {
        return unifier().toString(term, n);
    }

    ///////////////////////////////////////////
    // class Result
    ///////////////////////////////////////////

    protected static class ImmutableResult<T> implements IUniDisunifier.Result<T> {

        private final T result;
        private final IUniDisunifier.Immutable unifier;

        public ImmutableResult(T result, IUniDisunifier.Immutable unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public T result() {
            return result;
        }

        @Override public IUniDisunifier.Immutable unifier() {
            return unifier;
        }

    }

    ///////////////////////////////////////////
    // class Transient
    ///////////////////////////////////////////

    protected static class Transient implements IUniDisunifier.Transient {

        private IUniDisunifier.Immutable unifier;

        public Transient(IUniDisunifier.Immutable unifier) {
            this.unifier = unifier;
        }

        @Override public boolean isFinite() {
            return unifier.isFinite();
        }

        @Override public boolean isEmpty() {
            return unifier.isEmpty();
        }

        @Override public boolean contains(ITermVar var) {
            return unifier.contains(var);
        }

        @Override public java.util.Set<ITermVar> varSet() {
            return unifier.varSet();
        }

        @Override public java.util.Set<ITermVar> freeVarSet() {
            return unifier.freeVarSet();
        }

        @Override public boolean isCyclic() {
            return unifier.isCyclic();
        }

        @Override public ITermVar findRep(ITermVar var) {
            return unifier.findRep(var);
        }

        @Override public boolean hasTerm(ITermVar var) {
            return unifier.hasTerm(var);
        }

        @Override public ITerm findTerm(ITerm term) {
            return unifier.findTerm(term);
        }

        @Override public ITerm findRecursive(ITerm term) {
            return unifier.findRecursive(term);
        }

        @Override public boolean isGround(ITerm term) {
            return unifier.isGround(term);
        }

        @Override public boolean isCyclic(ITerm term) {
            return unifier.isCyclic(term);
        }

        @Override public Set.Immutable<ITermVar> getVars(ITerm term) {
            return unifier.getVars(term);
        }

        @Override public TermSize size(ITerm term) {
            return unifier.size(term);
        }

        @Override public String toString(ITerm term) {
            return unifier.toString(term);
        }

        @Override public String toString(ITerm term, int n) {
            return unifier.toString(term, n);
        }

        @Override public Map.Immutable<ITermVar, ITerm> equalityMap() {
            return unifier.equalityMap();
        }

        @Override public Set.Immutable<Diseq> disequalities() {
            return unifier.disequalities();
        }

        @Override public Optional<IUnifier.Immutable> unify(ITerm term1, ITerm term2) throws OccursException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(term1, term2);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<? extends IUnifier.Immutable> unify(IUnifier other) throws OccursException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(other);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<IUnifier.Immutable> unify(IUniDisunifier other) throws OccursException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(other);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<IUnifier.Immutable>
                unify(Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) throws OccursException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(equalities);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public boolean disunify(Iterable<ITermVar> universal, ITerm term1, ITerm term2) {
            final Optional<IUniDisunifier.Immutable> result = unifier.disunify(universal, term1, term2);
            return result.map(ud -> {
                unifier = ud;
                return ud;
            }).isPresent();
        }

        @Override public Optional<IUnifier.Immutable> diff(ITerm term1, ITerm term2) {
            return unifier.diff(term1, term2);
        }

        @Override public ISubstitution.Immutable retain(ITermVar var) {
            final IUniDisunifier.Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result =
                    unifier.retain(var);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable retainAll(Iterable<ITermVar> vars) {
            final IUniDisunifier.Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result =
                    unifier.retainAll(vars);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable remove(ITermVar var) {
            final IUniDisunifier.Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result =
                    unifier.remove(var);
            unifier = result.unifier();
            return result.result();
        }

        @Override public ISubstitution.Immutable removeAll(Iterable<ITermVar> vars) {
            final IUniDisunifier.Result<mb.nabl2.terms.substitution.ISubstitution.Immutable> result =
                    unifier.removeAll(vars);
            unifier = result.unifier();
            return result.result();
        }

        @Override public IUniDisunifier.Immutable freeze() {
            return unifier;
        }

        @Override public String toString() {
            return unifier.toString();
        }

    }

}