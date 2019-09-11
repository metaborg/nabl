package mb.statix.random.predicate;

import org.metaborg.util.functions.Predicate1;

import mb.statix.constraints.CUser;

public class IsGen implements Predicate1<CUser> {

    @Override public boolean test(CUser c) {
        return c.name().startsWith("gen_");
    };

    @Override public String toString() {
        return "is-gen";
    }

}