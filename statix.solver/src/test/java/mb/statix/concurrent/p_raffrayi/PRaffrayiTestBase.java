package mb.statix.concurrent.p_raffrayi;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.metaborg.util.task.NullCancel;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.IInitialState;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.solver.StatixDifferOps;
import mb.statix.scopegraph.terms.Scope;

public abstract class PRaffrayiTestBase {

    private final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

    protected <R> IFuture<IUnitResult<Scope, ITerm, ITerm, R>> run(String id, ITypeChecker<Scope, ITerm, ITerm, R> typeChecker,
            Iterable<ITerm> edgeLabels) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, new StatixDifferOps(), new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, new NullCancel());
    }

    protected <R> IFuture<IUnitResult<Scope, ITerm, ITerm, R>> run(String id, ITypeChecker<Scope, ITerm, ITerm, R> typeChecker,
            Iterable<ITerm> edgeLabels, IInitialState<Scope, ITerm, ITerm, R> initialState) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, initialState, new StatixDifferOps(), new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, initialState, new NullCancel());
    }
    
    @BeforeClass public static void setupTest() {
        for(String lname : Arrays.asList(Logger.ROOT_LOGGER_NAME, "mb.statix", "mb.statix.concurrent")) {
            Logger logger = (Logger) LoggerFactory.getLogger(lname);
            logger.setLevel(ch.qos.logback.classic.Level.TRACE);
        }
    }

}
