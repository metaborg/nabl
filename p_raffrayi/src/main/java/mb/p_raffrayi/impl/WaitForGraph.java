package mb.p_raffrayi.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.collection.Sets;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class WaitForGraph<P, T> {

    private static final ILogger logger = LoggerUtils.logger(WaitForGraph.class);

    private MultiSet.Transient<T> waitFors = MultiSet.Transient.of();
    private Map<P, MultiSet<T>> waitForsByProcess = new HashMap<>();

    private MultiSet.Transient<P> waits = MultiSet.Transient.of();

    public boolean isWaiting() {
        return !waitFors.isEmpty() || !waits.isEmpty();
    }

    public boolean isWaitingFor(T token) {
        return waitFors.contains(token);
    }

    public boolean isWaitingFor(P from, T token) {
        return waitForsByProcess.getOrDefault(from, MultiSet.Immutable.of()).contains(token);
    }

    public int countWaitingFor(P from, T token) {
        return waitForsByProcess.getOrDefault(from, MultiSet.Immutable.of()).count(token);
    }

    public MultiSet.Immutable<T> getTokens(P unit) {
        return MultiSet.Immutable.copyOf(waitForsByProcess.getOrDefault(unit, MultiSet.Immutable.of()));
    }

    public MultiSet<T> getTokens() {
        return waitFors;
    }

    public Set<P> dependencies() {
        return Sets.union(waitForsByProcess.keySet(), waits.elementSet());
    }

    public boolean waitFor(P process, T token) {
        boolean newDependency = !waitForsByProcess.containsKey(process) && !waits.contains(process);
        logger.debug("wait for {}/{}", process, token);
        waitFors.add(token);
        ((MultiSet.Transient<T>) waitForsByProcess.computeIfAbsent(process, __ -> MultiSet.Transient.of())).add(token);
        return newDependency;
    }

    protected boolean granted(P process, T token) {
        final MultiSet.Transient<T> tokens = (MultiSet.Transient<T>) waitForsByProcess.get(process);
        if(tokens == null || !tokens.contains(token)) {
            logger.error("not waiting for granted {}/{}", process, token);
            throw new IllegalStateException("not waiting for granted " + process + "/" + token);
        }
        logger.debug("granted {} by {}", token, process);
        waitFors.remove(token);
        tokens.remove(token);
        if(tokens.isEmpty()) {
            waitForsByProcess.remove(process);
            return !waits.contains(process); // dependency removed
        }
        return false;
    }

    public boolean waitFor(P process) {
        logger.debug("tokenless wait for {}", process);
        int oldCount = waits.add(process, 1);
        return oldCount == 0 && !waitForsByProcess.containsKey(process);
    }

    public boolean granted(P process) {
        logger.debug("tokenless wait for {}", process);
        int oldCount = waits.remove(process, 1);
        if(oldCount == 0) {
            logger.error("not waiting for granted {}", process);
            throw new IllegalStateException("not waiting for granted " + process);
        }
        return oldCount == 1 && !waitForsByProcess.containsKey(process);
    }

    @Override public String toString() {
        return "WaitForGraph{" +  waitForsByProcess + ", " + waits + "}";
    }

}
