package mb.nabl2.util;

import mb.nabl2.terms.ITerm;

@FunctionalInterface
public interface TermFormatter {

    String format(ITerm term);

    default String format(Iterable<? extends ITerm> terms) {
        return format(terms, ", ");
    }
    default String format(Iterable<? extends ITerm> terms, String sep) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(ITerm term : terms) {
            if(!first) {
                sb.append(sep);
            }
            first = false;
            sb.append(format(term));
        }
        return sb.toString();
    }

}