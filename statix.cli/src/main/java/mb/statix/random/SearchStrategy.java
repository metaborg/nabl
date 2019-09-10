package mb.statix.random;

import java.util.Random;
import java.util.stream.Stream;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public abstract class SearchStrategy<I, O> {

    private static final ILogger log = LoggerUtils.logger(SearchStrategy.class);

    public final Stream<SearchNode<O>> apply(Random rnd, int size, I input, SearchNode<?> parent) {
        if(size <= 0) {
            log.info("Fail: size is empty");
            return Stream.empty();
        }
        return doApply(rnd, size, input, parent);
    }

    protected abstract Stream<SearchNode<O>> doApply(Random rnd, int size, I input, SearchNode<?> parent);

}