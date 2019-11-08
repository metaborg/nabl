package mb.statix.generator.predicate;

import org.metaborg.util.functions.Predicate1;

public class Any<C> implements Predicate1<C> {

    @Override public boolean test(C c) {
        return true;
    };

    @Override public String toString() {
        return "any";
    }

}