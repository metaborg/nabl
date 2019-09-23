package mb.statix.random.predicate;

import java.util.regex.Pattern;

import org.metaborg.util.functions.Predicate1;

import mb.statix.constraints.CUser;

public class Match implements Predicate1<CUser> {

    private final Pattern pattern;

    public Match(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    @Override public boolean test(CUser c) {
        return pattern.matcher(c.name()).matches();
    };

    @Override public String toString() {
        return "match(" + pattern + ")";
    }

}