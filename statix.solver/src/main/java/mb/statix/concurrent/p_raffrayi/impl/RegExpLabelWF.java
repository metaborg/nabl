package mb.statix.concurrent.p_raffrayi.impl;

import java.util.Optional;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.p_raffrayi.nameresolution.LabelWF;

public class RegExpLabelWF implements LabelWF<ITerm> {

    private final IRegExpMatcher<ITerm> re;

    public RegExpLabelWF(IRegExpMatcher<ITerm> re) {
        this.re = re;
    }

    @Override public Optional<LabelWF<ITerm>> step(ITerm l) {
        final IRegExpMatcher<ITerm> re = this.re.match(l);
        if(re.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(new RegExpLabelWF(re));
        }
    }

    @Override public boolean accepting() {
        return re.isAccepting();
    }

    @Override public String toString() {
        return re.toString();
    }

    public static RegExpLabelWF of(IRegExpMatcher<ITerm> re) {
        return new RegExpLabelWF(re);
    }

}