package mb.nabl2.util;

import mb.nabl2.terms.ITerm;

@FunctionalInterface
public interface TermFormatter {

    String apply(ITerm term);

    default String apply(Iterable<? extends ITerm> terms) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(ITerm term : terms) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(apply(term));
        }
        return sb.toString();
    }

}