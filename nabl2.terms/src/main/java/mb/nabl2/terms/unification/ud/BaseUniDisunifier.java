package mb.nabl2.terms.unification.ud;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Optional;

import org.metaborg.util.functions.Predicate1;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.SpecializedTermFormatter;
import mb.nabl2.terms.unification.TermSize;
import mb.nabl2.terms.unification.u.IUnifier;

public abstract class BaseUniDisunifier implements IUniDisunifier, Serializable {

    private static final long serialVersionUID = 42L;

    protected abstract IUnifier.Immutable unifier();

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

    @Override public boolean isCyclic() {
        return unifier().isCyclic();
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

    @Override public String toString(final ITerm term, SpecializedTermFormatter specializedTermFormatter) {
        return unifier().toString(term, specializedTermFormatter);
    }

    @Override public String toString(final ITerm term, int depth, SpecializedTermFormatter specializedTermFormatter) {
        return unifier().toString(term, depth, specializedTermFormatter);
    }

    ///////////////////////////////////////////
    // class Result
    ///////////////////////////////////////////

    protected static class ImmutableResult<T> implements IUniDisunifier.Result<T> {

        private final T result;
        private final PersistentUniDisunifier.Immutable unifier;

        public ImmutableResult(T result, PersistentUniDisunifier.Immutable unifier) {
            this.result = result;
            this.unifier = unifier;
        }

        @Override public T result() {
            return result;
        }

        @Override public PersistentUniDisunifier.Immutable unifier() {
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

        @Override public Set.Immutable<ITermVar> domainSet() {
            return unifier.domainSet();
        }

        @Override public Set.Immutable<ITermVar> rangeSet() {
            return unifier.rangeSet();
        }

        @Override public Set.Immutable<ITermVar> varSet() {
            return unifier.varSet();
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

        @Override public String toString(ITerm term, SpecializedTermFormatter specializedTermFormatter) {
            return unifier.toString(term, specializedTermFormatter);
        }

        @Override public String toString(ITerm term, int depth, SpecializedTermFormatter specializedTermFormatter) {
            return unifier.toString(term, depth, specializedTermFormatter);
        }

        @Override public Set.Immutable<Diseq> disequalities() {
            return unifier.disequalities();
        }

        @Override public Optional<IUnifier.Immutable> unify(ITerm term1, ITerm term2, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(term1, term2, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<? extends IUnifier.Immutable> unify(IUnifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(other, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<IUnifier.Immutable> unify(IUniDisunifier other, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.uniDisunify(other, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<IUnifier.Immutable> unify(
                Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities, Predicate1<ITermVar> isRigid)
                throws OccursException, RigidException {
            final Optional<IUniDisunifier.Result<IUnifier.Immutable>> result = unifier.unify(equalities, isRigid);
            return result.map(r -> {
                unifier = r.unifier();
                return r.result();
            });
        }

        @Override public Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, ITerm term1, ITerm term2,
                Predicate1<ITermVar> isRigid) throws RigidException {
            final Optional<IUniDisunifier.Result<Optional<Diseq>>> result =
                    unifier.disunify(universal, term1, term2, isRigid);
            return result.map(ud -> {
                unifier = ud.unifier();
                return ud.result();
            });
        }

        @Override public Optional<Optional<Diseq>> disunify(Iterable<ITermVar> universal, IUnifier.Immutable diseqs,
                Predicate1<ITermVar> isRigid) throws RigidException {
            final Optional<IUniDisunifier.Result<Optional<Diseq>>> result =
                    unifier.disunify(universal, diseqs, isRigid);
            return result.map(ud -> {
                unifier = ud.unifier();
                return ud.result();
            });
        }

        @Override public Optional<IUnifier.Immutable> diff(ITerm term1, ITerm term2) {
            return unifier.diff(term1, term2);
        }

        @Override public boolean equal(ITerm term1, ITerm term2) {
            return unifier.equal(term1, term2);
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