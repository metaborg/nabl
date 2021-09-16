package mb.p_raffrayi.actors.deadlock;

import org.metaborg.util.collection.MultiSet;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;

/**
 * Implementation of Chandy et al.'s communication deadlock detection algorithm ([1], §4).
 *
 * Open questions:
 * <ul>
 * <li>What happens if the dependent set of an idle host contains itself?
 * <li>Is deadlock detected if an idle host only depends on itself?
 * <li>What happens if the dependent set of an idle host is empty? What should happen?
 * </ul>
 *
 * [1] Chandy, K. Mani, Jayadev Misra, and Laura M. Haas. “Distributed Deadlock Detection.” ACM Transactions on Computer
 * Systems 1, no. 2 (May 1, 1983): 144–156. https://doi.org/10.1145/357360.357365.
 */
public class ChandyMisraHaas<P> {

    private static final ILogger logger = LoggerUtils.logger(ChandyMisraHaas.class);

    enum State {
        IDLE, EXECUTING
    }

    private final Host<P> self;
    private final Action1<java.util.Set<P>> deadlockHandler;

    private State state;

    private final MultiSet.Transient<P> latest;
    private final Map.Transient<P, P> engager;
    private final MultiSet.Transient<P> num;
    private SetMultimap.Transient<P, P> wait;

    public ChandyMisraHaas(Host<P> self, Action1<java.util.Set<P>> deadlockHandler) {
        this.self = self;
        this.deadlockHandler = deadlockHandler;
        this.state = State.EXECUTING;
        this.latest = MultiSet.Transient.of();
        this.engager = Map.Transient.of();
        this.num = MultiSet.Transient.of();
        this.wait = SetMultimap.Transient.of();
    }

    /**
     * Initiate query computation on becoming idle.
     */
    public boolean idle() {
        if(state.equals(State.IDLE)) {
            return false;
        }
        logger.debug("{} idle", self);
        final P i = self.process();
        state = State.IDLE;
        latest.add(i);
        wait.__insert(i, i);
        final java.util.Set<P> S = self/*i*/.dependentSet();
        for(P j : S) {
            self/*i*/.query(j, i, latest.count(i));
        }
        num.set(i, S.size());
        return true;
    }

    /**
     * Becoming executing.
     */
    public boolean exec() {
        if(state.equals(State.EXECUTING)) {
            return false;
        }
        logger.debug("{} exec", self);
        @SuppressWarnings("unused") final P k = self.process();
        state = State.EXECUTING;
        wait = SetMultimap.Transient.of();
        return true;
    }

    /**
     * Receive query.
     *
     * @param i
     *            Idle host P_i.
     * @param m
     *            Sequence number m.
     * @param j
     *            Sending host P_j.
     */
    public void query(P i, int m, P j) {
        if(state.equals(State.EXECUTING)) {
            return;
        }
        // logger.debug("{} query {}.{} from {}", self, i, m, j);
        final P k = self.process();
        logger.debug("query(i={}, m={}, j={}, k={})", i, m, j, k);
        if(m > latest.count(i)) {
            latest.set(i, m);
            engager.put(i, j);
            wait.__insert(i, k);
            final java.util.Set<P> S = self/*k*/.dependentSet();
            for(P r : S) {
                self/*k*/.query(r, i, m);
            }
            num.set(i, S.size());
        } else if(wait.containsKey(i) && m == latest.count(i)) {
            self/*k*/.reply(j, i, m, wait.get(i));
        }
    }

    /**
     * Receive reply.
     *
     * @param i
     *            Idle host P_i.
     * @param m
     *            Sequence number m.
     * @param r
     *            Replying hosts R.
     */
    public void reply(P i, int m, java.util.Set<P> R, P r) {
        if(state.equals(State.EXECUTING)) {
            return;
        }
        // logger.debug("{} reply {}.{} from {}/{}", self, i, m, r, R);
        final P k = self.process();
        logger.debug("reply(i={}, m={}, r={}, k={})", i, m, r, k);
        if(m == latest.count(i) && wait.containsKey(i)) {
            for(P tr : R) {
                wait.__insert(i, tr);
            }
            if(num.remove(i) == 1) {
                final Set.Immutable<P> Q = wait.get(i);
                if(i.equals(k)) {
                    logger.debug("{} deadlocked with {}", self, Q);
                    deadlockHandler.apply(Q);
                } else {
                    P j = engager.get(i);
                    self/*k*/.reply(j, i, m, Q);
                }
            }
        }
    }

    public interface Host<P> {

        P process();

        java.util.Set<P> dependentSet();

        /**
         * Query.
         *
         * @param k
         *            Receiving host P_k.
         * @param i
         *            Idle host P_i.
         * @param m
         *            Sequence number m.
         */
        void query(P k, P i, int m);

        /**
         * Reply.
         *
         * @param k
         *            Receiving host P_k.
         * @param i
         *            Idle host P_i.
         * @param m
         *            Sequence number m.
         * @param r
         *            Replying hosts R.
         */
        void reply(P k, P i, int m, java.util.Set<P> R);

    }

}
