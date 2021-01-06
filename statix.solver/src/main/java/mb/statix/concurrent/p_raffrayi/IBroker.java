package mb.statix.concurrent.p_raffrayi;

import org.metaborg.util.unit.Unit;

import mb.statix.concurrent.actors.futures.IFuture;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D> {

    <R> IFuture<IUnitResult<S, L, D, R>> add(String id, ITypeChecker<S, L, D, R> unitChecker);

    IFuture<Unit> result();

}