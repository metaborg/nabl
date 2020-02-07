package mb.statix.generator;

import mb.statix.generator.nodes.SearchNodes;
import mb.statix.spec.Spec;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The default search context implementation.
 */
public class DefaultSearchContext implements SearchContext {

    private final AtomicInteger nodeId = new AtomicInteger();
    private final Spec spec;
    private final Random random;

    public DefaultSearchContext(Spec spec, long randomSeed) {

        this.spec = spec;
        this.random = new Random(randomSeed);
    }

    public DefaultSearchContext(Spec spec) {
        this(spec, System.currentTimeMillis());
    }

    @Override
    public Spec spec() {
        return this.spec;
    }

    @Override
    public Random rnd() {
        return this.random;
    }

    @Override
    public int nextNodeId() {
        return this.nodeId.incrementAndGet();
    }

    @Override
    public void failure(SearchNodes<?> nodes) {

    }

}