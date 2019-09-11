package mb.statix.random.predicate;

import org.metaborg.util.functions.Predicate1;

public class Not<C> implements Predicate1<C> {

    private final Predicate1<C> pred;

    public Not(Predicate1<C> pred) {
        this.pred = pred;
    }

    @Override public boolean test(C c) {
        return !pred.test(c);
    };

    @Override public String toString() {
        return "not(" + pred.toString() + ")";
    }

}