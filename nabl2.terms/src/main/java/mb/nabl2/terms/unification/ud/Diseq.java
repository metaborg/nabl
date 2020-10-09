package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.PersistentUnifier;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.Tuple3;

public class Diseq {

    @SuppressWarnings("unused") private static ILogger logger = LoggerUtils.logger(Diseq.class);

    private final Set.Immutable<ITermVar> universals;
    private final IUnifier.Immutable diseqs;

    private Diseq(Set.Immutable<ITermVar> universals, IUnifier.Immutable diseqs) {
        this.universals = CapsuleUtil.toSet(universals);
        this.diseqs = diseqs;
    }

    /**
     * Universally quantified vars in this disequality
     */
    public Set.Immutable<ITermVar> universals() {
        return universals;
    }

    public IUnifier.Immutable disequalities() {
        return diseqs;
    }

    /**
     * Free variables in this disequality.
     */
    public Set.Immutable<ITermVar> freeVarSet() {
        return Set.Immutable.subtract(diseqs.freeVarSet(), universals);
    }

    public boolean isEmpty() {
        return diseqs.isEmpty();
    }

    /**
     * Restricted variables in this disequality.
     */
    public Set.Immutable<ITermVar> varSet() {
        return Set.Immutable.subtract(diseqs.varSet(), universals);
    }

    public Tuple3<Set<ITermVar>, ITerm, ITerm> toTuple() {
        ImmutableList.Builder<ITerm> lefts = ImmutableList.builder();
        ImmutableList.Builder<ITerm> rights = ImmutableList.builder();
        diseqs.equalityMap().forEach((v, t) -> {
            lefts.add(v);
            rights.add(t);
        });
        return Tuple3.of(universals, B.newTuple(lefts.build()), B.newTuple(rights.build()));
    }

    public Diseq apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable localSubst = subst.removeAll(universals);
        final IUnifier.Transient newDiseqs = PersistentUnifier.Immutable.of(diseqs.isFinite()).melt();
        diseqs.equalityMap().forEach((v, t) -> {
            try {
                if(!newDiseqs.unify(v, localSubst.apply(t)).isPresent()) {
                    throw new IllegalArgumentException("Applying substitution failed unexpectedly.");
                }
            } catch(OccursException e) {
                throw new IllegalArgumentException("Applying substitution failed unexpectedly.");
            }
        });
        return new Diseq(universals, newDiseqs.freeze());
    }

    /**
     * Remove variables. Return the new, reduced disequality, or none if it is now empty.
     */
    public Optional<Diseq> removeAll(Iterable<ITermVar> vars) {
        final IUnifier.Immutable newDiseqs = diseqs.removeAll(vars).unifier();
        if(newDiseqs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Diseq(universals, newDiseqs));
    }

    public Diseq rename(IRenaming renaming) {
        if(renaming.isEmpty()) {
            return this;
        }
        final Set.Immutable<ITermVar> universals =
                this.universals.stream().map(renaming::rename).collect(CapsuleCollectors.toSet());
        final IUnifier.Immutable diseqs = this.diseqs.rename(renaming);
        return new Diseq(universals, diseqs);
    }

    public boolean implies(Diseq other) {
        final Set.Immutable<ITermVar> freeVars = Set.Immutable.union(freeVarSet(), other.freeVarSet());
        final FreshVars fv = new FreshVars(freeVars);

        final Diseq diseq = this.rename(fv.fresh(universals));
        final Diseq otherDiseq = other.rename(fv.fresh(other.universals));

        final Set.Immutable<ITermVar> rigidVars = freeVars.__insertAll(otherDiseq.universals);
        try {
            final IUnifier.Result<? extends IUnifier.Immutable> ur;
            if((ur = diseq.diseqs.unify(otherDiseq.diseqs, rigidVars::contains).orElse(null)) == null) {
                return false;
            }
            if(ur.result().isEmpty()) {
                return true;
            }
            final IUnifier.Result<? extends ISubstitution.Immutable> rr;
            rr = ur.result().removeAll(diseq.universals);
            return rr.unifier().isEmpty();
        } catch(OccursException | RigidException e) {
            return false;
        }
    }

    @Override public String toString() {
        return toTuple().apply((us, v, t) -> {
            if(us.isEmpty()) {
                return v + " != " + t;
            } else {
                return "forall " + us + " . " + v + " != " + t;
            }
        });
    }

    public static Diseq of(Iterable<ITermVar> universals, IUnifier.Immutable diseqs) {
        final IUnifier.Immutable newDiseqs = diseqs.removeAll(universals).unifier();
        final Set.Immutable<ITermVar> newUniversals =
                Set.Immutable.intersect(CapsuleUtil.toSet(universals), newDiseqs.freeVarSet());
        return new Diseq(newUniversals, newDiseqs);
    }

    public static Optional<Diseq> of(Iterable<ITermVar> universals, ITerm left, ITerm right) {
        try {
            return PersistentUnifier.Immutable.of().unify(left, right).map(r -> {
                return of(universals, r.unifier());
            });
        } catch(OccursException e) {
            return Optional.empty();
        }
    }

}