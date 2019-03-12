package mb.nabl2.util;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;

@FunctionalInterface
public interface TermFormatter {

    String format(ITerm term);

    default String format(Iterable<? extends ITerm> terms) {
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(ITerm term : terms) {
            if(!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(format(term));
        }
        return sb.toString();
    }

    default TermFormatter removeAll(@SuppressWarnings("unused") Iterable<ITermVar> vars) {
        return this;
    }

}