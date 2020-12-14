package mb.statix.concurrent.actors.deadlock;

import java.util.Map;
import java.util.Set;

import org.metaborg.util.functions.Action1;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import mb.statix.concurrent.actors.deadlock.ChandyMisraHaas.Host;

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
public class ChandyMisraHaas<P extends Host<P>> {

    enum State {
        IDLE, EXECUTING
    }

    private final P self;
    private final Action1<P> deadlockHandler;

    private State state;

    private final Map<P, Integer> latest;
    private final Map<P, P> engager;
    private final Map<P, Integer> num;
    private final Set<P> wait;

    public ChandyMisraHaas(P self, Action1<P> deadlockHandler) {
        this.self = self;
        this.deadlockHandler = deadlockHandler;
        this.state = State.EXECUTING;
        this.latest = Maps.newHashMap();
        this.num = Maps.newHashMap();
        this.engager = Maps.newHashMap();
        this.wait = Sets.newHashSet();
    }

    /**
     * Initiate query computation on idle host P_i.
     */
    public void idle() {
        final P i = self;
        if(!state.equals(State.EXECUTING)) {
            throw new IllegalStateException();
        }
        state = State.IDLE;
        latest.put(i, latest.get(i) + 1);
        wait.add(i);
        final Set<P> S = i.dependentSet();
        for(P j : S) {
            j.query(i, latest.get(i), i);
        }
        num.put(i, S.size());
    }

    /**
     * Becoming executing on host P_k.
     */
    public void exec() {
        @SuppressWarnings("unused") final P k = self;
        if(!state.equals(State.IDLE)) {
            throw new IllegalStateException();
        }
        wait.clear();
        state = State.EXECUTING;
    }

    /**
     * Receive query on host P_k.
     * 
     * @param i
     *            Idle host P_i.
     * @param m
     *            Sequence number m.
     * @param j
     *            Sending host P_j.
     */
    public void query(P i, int m, P j) {
        final P k = self;
        if(state.equals(State.EXECUTING)) {
            return;
        }
        if(m > latest.getOrDefault(i, 0)) {
            latest.put(i, m);
            engager.put(i, j);
            wait.add(i);
            final Set<P> S = k.dependentSet();
            for(P r : S) {
                r.query(i, m, k);
            }
            num.put(i, S.size());
        } else if(wait.contains(i) && m == latest.get(i)) {
            j.reply(i, m, k);
        }
    }

    /**
     * Receive reply on host P_k.
     * 
     * @param i
     *            Idle host P_i.
     * @param m
     *            Sequence number m.
     * @param r
     *            Replying host P_r.
     */
    public void reply(P i, int m, P r) {
        final P k = self;
        if(state.equals(State.EXECUTING)) {
            return;
        }
        if(m == latest.get(i) && wait.contains(i)) {
            num.put(i, num.get(i) - 1);
            if(num.get(i) == 0) {
                if(i.equals(k)) {
                    deadlockHandler.apply(k);
                } else {
                    P j = engager.get(i);
                    j.reply(i, m, k);
                }
            }
        }
    }

    public interface Host<P extends Host<P>> {

        Set<P> dependentSet();

        void query(P i, int m, P j);

        void reply(P i, int m, P r);

    }

}