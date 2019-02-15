package mb.statix.taico.scopegraph;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.util.IOwnable;

/**
 * Interface to represent an ITerm that can be owned.
 */
public interface IOwnableTerm extends IOwnable {
    
    ITerm getTerm();
}
