package mb.scopegraph.ecoop21;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import mb.scopegraph.regexp.IRegExpMatcher;

public class RegExpLabelWf<L> implements LabelWf<L>, Serializable {

    private static final long serialVersionUID = 42L;

    private final IRegExpMatcher<L> re;

    public RegExpLabelWf(IRegExpMatcher<L> re) {
        this.re = re;
    }

    @Override public Optional<LabelWf<L>> step(L l) {
        final IRegExpMatcher<L> re = this.re.match(l);
        if(re.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new RegExpLabelWf<>(re));
        }
    }

    @Override public boolean accepting() {
        return re.isAccepting();
    }

    @Override public int hashCode() {
        return Objects.hash(re);
    }

    @Override public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        RegExpLabelWf<?> other = (RegExpLabelWf<?>) obj;
        return Objects.equals(re, other.re);
    }

    @Override public String toString() {
        return re.toString();
    }

    public static <L> RegExpLabelWf<L> of(IRegExpMatcher<L> re) {
        return new RegExpLabelWf<>(re);
    }

}