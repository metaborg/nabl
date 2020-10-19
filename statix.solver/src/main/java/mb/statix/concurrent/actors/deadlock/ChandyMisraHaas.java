package mb.statix.concurrent.actors.deadlock;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import mb.statix.concurrent.actors.deadlock.ChandyMisraHaas.Host;

/**
 * Implementation of the communication deadlock detection algorithm by Chandy, Misra, and Haas (1983), §4.
 */
public class ChandyMisraHaas<P extends Host<P>> {

    enum State {
        IDLE, EXECUTING
    }

    private final P self;
    private final State state;

    private final Map<P, Integer> latest;
    private final Map<P, P> engager;
    private final Map<P, Integer> num;
    private final Set<P> wait;

    public ChandyMisraHaas(P self) {
        this.self = self;
        this.state = State.EXECUTING;
        this.latest = Maps.newHashMap();
        this.num = Maps.newHashMap();
        this.engager = Maps.newHashMap();
        this.wait = Sets.newHashSet();
    }

    /**
     * Initiate query computation in idle host P_i.
     */
    void idle() {
        if(!state.equals(State.EXECUTING)) {
            throw new IllegalStateException();
        }
        final P i = self;
        latest.put(i, latest.get(i) + 1);
        wait.add(i);
        final Set<P> S = i.dependentSet();
        for(P j : S) {
            j.query(i, latest.get(i), i);
        }
        num.put(i, S.size());
    }

    /**
     * On becoming executing in host P_k.
     */
    void exec() {
        if(!state.equals(State.IDLE)) {
            throw new IllegalStateException();
        }
        @SuppressWarnings("unused") final P k = self;
        wait.clear();
    }

    /**
     * Receive query in idle host P_k.
     * 
     * @param i
     *            Ilde host P_i.
     * @param m
     *            Sequence number m.
     * @param j
     *            Sender host P_j.
     */
    void query(P i, int m, P j) {
        if(state.equals(State.EXECUTING)) {
            return;
        }
        final P k = self;
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
     * Receive query in idle host P_k.
     * 
     * @param i
     *            Idle host P_i.
     * @param m
     *            Sequence number m.
     * @param j
     *            Sender host P_j.
     */
    void reply(P i, int m, P r) {
        if(state.equals(State.EXECUTING)) {
            return;
        }
        final P k = self;
        if(m == latest.get(i) && wait.contains(i)) {
            num.put(i, num.get(i) - 1);
            if(num.get(i) == 0) {
                if(i.equals(k)) {
                    // k is deadlocked---on what?
                } else {
                    P j = engager.get(i);
                    j.reply(i, m, k);
                }
            }
        }
    }

    interface Host<P extends Host<P>> {

        Set<P> dependentSet();

        void query(P i, int m, P j);

        void reply(P i, int m, P r);

    }

}