package mb.statix.p_raffrayi;

import org.metaborg.util.task.ICancel;

import mb.statix.actors.futures.IFuture;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D> {

    void add(String id, ITypeChecker<S, L, D, ?> unitChecker);

    IFuture<IResult<S, L, D>> run(ICancel cancel);

}