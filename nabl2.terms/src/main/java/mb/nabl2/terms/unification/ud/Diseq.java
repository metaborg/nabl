package mb.nabl2.terms.unification.ud;

import static mb.nabl2.terms.build.TermBuild.B;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;

public class Diseq {

    private final Set.Immutable<ITermVar> universals;
    private final Map.Immutable<ITermVar, ITerm> diseqs;

    public Diseq(Iterable<ITermVar> universals, java.util.Map<ITermVar, ITerm> eqMap) {
        this.universals = CapsuleUtil.toSet(universals);
        this.diseqs = CapsuleUtil.toMap(eqMap);
    }

    /**
     * Universally quantified vars in this disequality
     */
    public Set.Immutable<ITermVar> universals() {
        return universals;
    }

    public Map.Immutable<ITermVar, ITerm> disequalities() {
        return diseqs;
    }

    /**
     * Free variables in this disequality.
     */
    public java.util.Set<ITermVar> freeVars() {
        return Sets.difference(diseqs.keySet(), universals).immutableCopy();
    }

    public Tuple3<Set<ITermVar>, ITerm, ITerm> toTuple() {
        ImmutableList.Builder<ITerm> lefts = ImmutableList.builder();
        ImmutableList.Builder<ITerm> rights = ImmutableList.builder();
        diseqs.forEach((v, t) -> {
            lefts.add(v);
            rights.add(t);
        });
        return ImmutableTuple3.of(universals, B.newTuple(lefts.build()), B.newTuple(rights.build()));
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

}