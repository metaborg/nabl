package mb.statix.p_raffrayi;

import org.metaborg.util.task.ICancel;

import mb.statix.actors.futures.IFuture;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D, R> {

    void add(String id, ITypeChecker<S, L, D, R> unitChecker);

    IFuture<IResult<S, L, D, R>> run(ICancel cancel);

}