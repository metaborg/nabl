package mb.statix.random.predicate;

import org.metaborg.util.functions.Predicate1;

import mb.statix.constraints.CUser;

public class IsType implements Predicate1<CUser> {

    @Override public boolean test(CUser c) {
        return c.name().startsWith("is_");
    };

    @Override public String toString() {
        return "is-type";
    }

}