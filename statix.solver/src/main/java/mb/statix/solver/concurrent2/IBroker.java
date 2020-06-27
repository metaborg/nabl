package mb.statix.solver.concurrent2;

import org.metaborg.util.task.ICancel;

import mb.statix.actors.IFuture;
import mb.statix.solver.concurrent2.impl.IUnitProtocol;

/**
 * Represents the whole system of type checkers to the outside.
 */
public interface IBroker<S, L, D> {

    void add(String id, ITypeChecker<S, L, D> unitChecker);

    IFuture<IResult<S, L, D>> run(ICancel cancel);

    IUnitProtocol<S, L, D> get(String id);
    
}