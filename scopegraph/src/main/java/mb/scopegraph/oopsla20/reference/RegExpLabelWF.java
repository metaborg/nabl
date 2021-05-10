package mb.scopegraph.oopsla20.reference;

import java.util.Optional;

import mb.scopegraph.regexp.IRegExpMatcher;

public class RegExpLabelWF<L> implements LabelWF<L> {

    private final IRegExpMatcher<L> re;

    private RegExpLabelWF(IRegExpMatcher<L> re) {
        this.re = re;
    }

    @Override public Optional<LabelWF<L>> step(L l) throws ResolutionException, InterruptedException {
        final IRegExpMatcher<L> re = this.re.match(l);
        if(re.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new RegExpLabelWF<>(re));
        }
    }

    @Override public boolean accepting() throws ResolutionException, InterruptedException {
        return re.isAccepting();
    }

    @Override public String toString() {
        return re.toString();
    }

    public static <L> RegExpLabelWF<L> of(IRegExpMatcher<L> re) {
        return new RegExpLabelWF<>(re);
    }

}