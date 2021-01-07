package mb.statix.concurrent.actors.deadlock;

import java.util.Map;
import java.util.Set;

import org.metaborg.util.functions.Action1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;

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
    private final Action1<Set<P>> deadlockHandler;

    private State state;

    private final Map<P, Integer> latest;
    private final Map<P, P> engager;
    private final Map<P, Integer> num;
    private final SetMultimap<P, P> wait;

    public ChandyMisraHaas(Host<P> self, Action1<Set<P>> deadlockHandler) {
        this.self = self;
        this.deadlockHandler = deadlockHandler;
        this.state = State.EXECUTING;
        this.latest = Maps.newHashMap();
        this.num = Maps.newHashMap();
        this.engager = Maps.newHashMap();
        this.wait = HashMultimap.create();
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
        latest.put(i, latest.getOrDefault(i, 0) + 1);
        wait.put(i, i);
        final Set<P> S = self/*i*/.dependentSet();
        for(P j : S) {
            self/*i*/.query(j, i, latest.get(i));
        }
        num.put(i, S.size());
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
        wait.clear();
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
        logger.debug("{} query {}.{} from {}", self, i, m, j);
        final P k = self.process();
        if(m > latest.getOrDefault(i, 0)) {
            latest.put(i, m);
            engager.put(i, j);
            wait.put(i, k);
            final Set<P> S = self/*k*/.dependentSet();
            for(P r : S) {
                self/*k*/.query(r, i, m);
            }
            num.put(i, S.size());
        } else if(wait.containsKey(i) && m == latest.get(i)) {
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
    public void reply(P i, int m, Set<P> R) {
        if(state.equals(State.EXECUTING)) {
            return;
        }
        logger.debug("{} reply {}.{} from {}", self, i, m, R);
        final P k = self.process();
        if(m == latest.get(i) && wait.containsKey(i)) {
            wait.putAll(i, R);
            final int num_i = num.put(i, num.get(i) - 1) - 1;
            if(num_i == 0) {
                final Set<P> Q = wait.get(i);
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

        Set<P> dependentSet();

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
        void reply(P k, P i, int m, Set<P> R);

    }

}