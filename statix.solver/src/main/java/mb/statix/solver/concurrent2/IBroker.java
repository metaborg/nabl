package mb.statix.solver.concurrent2;

import java.util.Map;

import org.metaborg.util.task.ICancel;

import mb.statix.solver.concurrent.util.IFuture;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D> {

    IFuture<IResult<S, L, D>> run(Map<String, ITypeChecker<S, L, D>> units, ICancel cancel);

}