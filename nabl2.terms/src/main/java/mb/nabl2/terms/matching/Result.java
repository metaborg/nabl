package mb.nabl2.terms.matching;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.Tuple2;

public class Result {

    private final ISubstitution.Immutable substitution;
    private final List<Tuple2<ITerm, ITerm>> equalities;

    Result(ISubstitution.Immutable substitution,
            Iterable<? extends Entry<? extends ITerm, ? extends ITerm>> equalities) {
        this.substitution = substitution;
        this.equalities = Streams.stream(equalities).map(Tuple2::of).collect(Collectors.toList());
    }

    public ISubstitution.Immutable substitution() {
        return substitution;
    }

    public List<Tuple2<ITerm, ITerm>> equalities() {
        return equalities;
    }

}