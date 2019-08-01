package mb.statix.solver.query;

import java.io.Serializable;
import java.util.Optional;

import mb.nabl2.regexp.IRegExpMatcher;
import mb.nabl2.terms.ITerm;
import mb.statix.scopegraph.reference.LabelWF;
import mb.statix.scopegraph.reference.ResolutionException;

public class RegExpLabelWF implements LabelWF<ITerm>, Serializable {
    private static final long serialVersionUID = 1L;

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
    
    @Override
    public boolean canStep(ITerm l) {
        return !this.re.match(l).isEmpty();
    }

    @Override public boolean accepting() throws ResolutionException, InterruptedException {
        return re.isAccepting();
    }
    
    public IRegExpMatcher<ITerm> getRegex() {
        return re;
    }

    @Override public String toString() {
        return re.toString();
    }
    
    @Override public int hashCode() {
        return re.hashCode();
    }
    
    @Override public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RegExpLabelWF)) return false;
        return re.equals(((RegExpLabelWF) obj).re);
    }

    public static RegExpLabelWF of(IRegExpMatcher<ITerm> re) {
        return new RegExpLabelWF(re);
    }

}