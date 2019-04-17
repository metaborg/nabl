package mb.statix.taico.solver;

import org.metaborg.util.functions.Function3;

import mb.nabl2.terms.ITerm;

@FunctionalInterface
public interface ICompleteness extends Function3<ITerm, ITerm, IMState, CompletenessResult> {
    
}
