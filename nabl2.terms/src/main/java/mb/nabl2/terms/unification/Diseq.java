package mb.nabl2.terms.unification;

import static mb.nabl2.terms.build.TermBuild.B;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class Diseq {

    final Map.Immutable<ITermVar, ITerm> diseqs;

    public Diseq(java.util.Map<ITermVar, ITerm> eqMap) {
        this.diseqs = CapsuleUtil.toMap(eqMap);
    }

    public Tuple2<ITerm, ITerm> toTuple() {
        ImmutableList.Builder<ITerm> lefts = ImmutableList.builder();
        ImmutableList.Builder<ITerm> rights = ImmutableList.builder();
        diseqs.forEach((v, t) -> {
            lefts.add(v);
            rights.add(t);
        });
        return ImmutableTuple2.of(B.newTuple(lefts.build()), B.newTuple(rights.build()));
    }

    @Override public String toString() {
        return toTuple().apply((v, t) -> v + " != " + t);
    }

}