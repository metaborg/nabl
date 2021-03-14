package mb.statix.concurrent.p_raffrayi.nameresolution;

import java.util.Optional;

import mb.nabl2.regexp.IRegExpMatcher;

public class RegExpLabelWf<L> implements LabelWf<L> {

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

    @Override public String toString() {
        return re.toString();
    }

    public static <L> RegExpLabelWf<L> of(IRegExpMatcher<L> re) {
        return new RegExpLabelWf<>(re);
    }

}