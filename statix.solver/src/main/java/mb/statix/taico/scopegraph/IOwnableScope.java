package mb.statix.taico.scopegraph;

import mb.nabl2.scopegraph.IScope;
import mb.nabl2.terms.IApplTerm;

/**
 * Interface to represent a scope that has an owner and is also a stratego term.
 */
public interface IOwnableScope extends IScope, IOwnableTerm, IApplTerm {

}