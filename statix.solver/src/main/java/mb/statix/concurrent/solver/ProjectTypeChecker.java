package mb.statix.concurrent.solver;

import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.ITypeChecker;
import mb.statix.concurrent.p_raffrayi.ITypeCheckerContext;
import mb.statix.scopegraph.terms.Scope;

public class ProjectTypeChecker implements ITypeChecker<Scope, ITerm, ITerm, Void> {

    @Override public IFuture<Void> run(ITypeCheckerContext<Scope, ITerm, ITerm, Void> unit, Scope root) {
        return CompletableFuture.of(null);
    }

}