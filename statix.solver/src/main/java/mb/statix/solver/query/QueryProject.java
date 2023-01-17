package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collector;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.tuple.Tuple2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import mb.nabl2.terms.ITerm;
import mb.nabl2.util.TermFormatter;

import static mb.nabl2.terms.build.TermBuild.B;
import mb.statix.spoofax.StatixTerms;

public enum QueryProject implements Serializable, Function1<ITerm, Optional<ITerm>> {
    FULL, TARGET_DATA, DATA;

    @Override public Optional<ITerm> apply(ITerm t) {
        switch(this) {
            case FULL:
                return Optional.of(t);
            case TARGET_DATA:
                return StatixTerms.pathTargetAndData(t).map(target -> B.newTuple(target._1(), target._2()));
            case DATA:
                return StatixTerms.pathTargetAndData(t).map(Tuple2::_2);
            default:
                throw new IllegalArgumentException("Unknown projection: " + this);
        }
    }

    public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("project ");
        sb.append(this.toString());
        return sb.toString();
    }

    @Override public String toString() {
        switch(this) {
            case FULL:
                return "*";
            case TARGET_DATA:
                return "target";
            case DATA:
                return "$";
            default:
                throw new IllegalArgumentException("Unknown projection: " + this);
        }
    }

    public Collector<ITerm, ?, ? extends Collection<ITerm>> collector() {
        switch(this) {
            case FULL:
                return ImmutableList.<ITerm>toImmutableList();
            default:
                return ImmutableSet.toImmutableSet();
        }
    }

}
