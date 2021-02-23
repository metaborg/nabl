package mb.statix.concurrent.p_raffrayi;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.metaborg.util.task.NullCancel;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import mb.nabl2.terms.ITerm;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.scopegraph.terms.Scope;

public abstract class PRaffrayiTestBase {

    private final IScopeImpl<Scope, ITerm> scopeImpl = new ScopeImpl();

    protected <L, R> IFuture<IUnitResult<Scope, L, ITerm, R>> run(String id, ITypeChecker<Scope, L, ITerm, R> typeChecker,
            Iterable<L> edgeLabels) {
        return Broker.debug(id, typeChecker, scopeImpl, edgeLabels, new NullCancel(), 0.3, 50);
        //return Broker.run(id, typeChecker, scopeImpl, edgeLabels, new NullCancel());
    }
    
    @BeforeClass public static void setupTest() {
        for(String lname : Arrays.asList(Logger.ROOT_LOGGER_NAME, "mb.statix", "mb.statix.concurrent")) {
            Logger logger = (Logger) LoggerFactory.getLogger(lname);
            logger.setLevel(ch.qos.logback.classic.Level.TRACE);
        }
    }

}
