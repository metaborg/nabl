package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.functions.Predicate1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.tuple.Tuple3;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.util.stream.CapsuleCollectors;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.IReplacement;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.RigidException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.terms.unification.u.IUnifier.Immutable;
import mb.nabl2.terms.unification.u.IUnifier.Result;
import mb.nabl2.terms.unification.u.PersistentUnifier;

public class Diseq implements Serializable {

    private static final long serialVersionUID = 42L;

    @SuppressWarnings("unused") private static ILogger logger = LoggerUtils.logger(Diseq.class);

    private final Set.Immutable<ITermVar> universals;
    private final IUnifier.Immutable diseqs;

    private final Set.Immutable<ITermVar> domainSetCache;
    private final Set.Immutable<ITermVar> freeVarSetCache;

    private Diseq(Set.Immutable<ITermVar> universals, IUnifier.Immutable diseqs) {
        this.universals = CapsuleUtil.toSet(universals);
        this.diseqs = diseqs;
        this.domainSetCache = this.diseqs.domainSet().__removeAll(this.universals);
        this.freeVarSetCache = this.diseqs.varSet().__removeAll(this.universals);
    }

    private Diseq(Set.Immutable<ITermVar> universals, IUnifier.Immutable diseqs, Set.Immutable<ITermVar> domainSetCache,
            Set.Immutable<ITermVar> freeVarSetCache) {
        this.universals = CapsuleUtil.toSet(universals);
        this.diseqs = diseqs;
        this.domainSetCache = domainSetCache;
        this.freeVarSetCache = freeVarSetCache;
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
        return freeVarSetCache;
    }

    /**
     * Returns if the disequality is empty. An empty disequality is logically false.
     */
    public boolean isEmpty() {
        return diseqs.isEmpty();
    }

    /**
     * Domain of this disequality, i.e., all variables restricted by the disequality.
     */
    public Set.Immutable<ITermVar> domainSet() {
        return domainSetCache;
    }

    public Tuple3<Set<ITermVar>, ITerm, ITerm> toTuple() {
        ImmutableList.Builder<ITerm> lefts = ImmutableList.builder();
        ImmutableList.Builder<ITerm> rights = ImmutableList.builder();
        for(ITermVar var : diseqs.domainSet()) {
            lefts.add(var);
            rights.add(diseqs.findTerm(var));
        }
        return Tuple3.of(universals, B.newTuple(lefts.build()), B.newTuple(rights.build()));
    }

    /**
     * Apply the given substitution to the disequality. Returns none if the disequality holds, or the Diseq object
     * otherwise.
     */
    public Optional<Diseq> apply(ISubstitution.Immutable subst) {
        final ISubstitution.Immutable localSubst = subst.removeAll(universals);

        final Diseq diseq;
        if(universals.isEmpty()) {
            diseq = this;
        } else {
            final FreshVars fv = new FreshVars(freeVarSet(), localSubst.rangeSet());
            diseq = this.rename(fv.fresh(this.universals));
        }

        final IUnifier.Transient newDiseqs = PersistentUnifier.Immutable.of(diseq.diseqs.isFinite()).melt();
        for(ITermVar var : diseq.diseqs.domainSet()) {
            try {
                if(!newDiseqs.unify(localSubst.apply(var), localSubst.apply(diseq.diseqs.findTerm(var))).isPresent()) {
                    return Optional.empty();
                }
            } catch(OccursException e) {
                return Optional.empty();
            }
        }
        final Set.Immutable<ITermVar> newUniversals = Set.Immutable.intersect(diseq.universals, newDiseqs.varSet());

        return Optional.of(new Diseq(newUniversals, newDiseqs.freeze()));
    }

    /**
     * Remove variables. Return the new, reduced disequality, or none if it is now empty.
     */
    public Diseq removeAll(Iterable<ITermVar> _vars) {
        Set.Immutable<ITermVar> vars = CapsuleUtil.toSet(_vars);
        if(!universals.isEmpty()) {
            vars = vars.__removeAll(universals);
        }
        final IUnifier.Immutable newDiseqs = diseqs.removeAll(vars).unifier();
        return new Diseq(universals, newDiseqs);
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

    public Diseq replace(IReplacement replacement) {
        if(replacement.isEmpty()) {
            return this;
        }
        return new Diseq(universals, diseqs.replace(replacement), domainSetCache, freeVarSetCache);
    }

    public boolean implies(Diseq other) {
        final Diseq diseq;
        final Diseq otherDiseq;
        if(this.universals.isEmpty() && other.universals.isEmpty()) {
            diseq = this;
            otherDiseq = other;
        } else {
            final Set.Immutable<ITermVar> freeVars = Set.Immutable.union(freeVarSet(), other.freeVarSet());
            final FreshVars fv = new FreshVars(freeVars);
            diseq = this.rename(fv.fresh(this.universals));
            otherDiseq = other.rename(fv.fresh(other.universals));
        }

        final Set.Immutable<ITermVar> universals = Set.Immutable.union(diseq.universals, otherDiseq.universals);
        final Predicate1<ITermVar> isRigid = v -> !universals.contains(v);
        try {
            final IUnifier.Result<? extends IUnifier.Immutable> ur;
            if((ur = otherDiseq.diseqs.unify(diseq.diseqs, isRigid).orElse(null)) == null) {
                return false;
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

    public static Diseq of(Iterable<ITermVar> _universals, IUnifier.Immutable diseqs) {
        final Set.Immutable<ITermVar> universals = CapsuleUtil.toSet(_universals);
        final IUnifier.Immutable newDiseqs = diseqs.removeAll(universals).unifier();
        final Set.Immutable<ITermVar> newUniversals = Set.Immutable.intersect(universals, newDiseqs.varSet());
        return new Diseq(newUniversals, newDiseqs);
    }

    /**
     * Create a new disequality. Returns none if the disequality holds, or a disequality object otherwise.
     */
    public static Optional<Diseq> of(Iterable<ITermVar> universals, ITerm left, ITerm right) {
        try {
            final Result<? extends Immutable> ur;
            if((ur = PersistentUnifier.Immutable.of().unify(left, right).orElse(null)) != null) {
                // unify succeeded
                return Optional.of(of(universals, ur.unifier()));
            } else {
                // unify failed, disequality holds
                return Optional.empty();
            }
        } catch(OccursException e) {
            // unify failed, disequality holds
            return Optional.empty();
        }
    }

}
