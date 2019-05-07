package mb.statix.solver.query;

import java.util.Optional;

import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.regexp.RegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;

public class RegExpLabelWF implements LabelWF<ITerm> {

    private final IRegExpMatcher<ITerm> re;

    private RegExpLabelWF(IRegExpMatcher<ITerm> re) {
        this.re = re;
    }

    @Override public Optional<LabelWF<ITerm>> step(ITerm l) throws ResolutionException, InterruptedException {
        final IRegExpMatcher<ITerm> re = this.re.match(l);
        if(re.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new RegExpLabelWF(re));
        }
    }

    @Override public boolean accepting() throws ResolutionException, InterruptedException {
        return re.isAccepting();
    }

    public static RegExpLabelWF of(IRegExp<ITerm> re) {
        return new RegExpLabelWF(RegExpMatcher.create(re));
    }

}