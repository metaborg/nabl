package mb.statix.actors.deadlock;

import static org.metaborg.util.unit.Unit.unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.metaborg.util.functions.Action1;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.Lists;

import mb.nabl2.util.collections.MultiSet;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.deadlock.Deadlock;
import mb.statix.concurrent.actors.deadlock.WaitForGraph;

public class WaitForGraphTest {

    private final static int SETS = 13;

    private static final Integer NODE_1 = 1;
    private static final Integer NODE_2 = 2;
    private static final Integer NODE_3 = 3;

    private static final String RES_A = "A";
    private static final String RES_B = "B";
    private static final String RES_C = "C";

    ///////////////////////////////////////////////////////////////////////////
    // Single node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testSelfDeadlock() {
        Trace trace1 = new Trace(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);

        trace1.suspendDeadlocked(new Object());

        runInterleavedSet(SETS, trace1);
    }

    @Test public void testSelfDeadlockTwoResources() {
        Trace trace1 = new Trace(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.waitFor(RES_B, NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);

        trace1.delivered(NODE_1);

        trace1.suspendDeadlocked(new Object());

        runInterleavedSet(SETS, trace1);
    }

    @Test public void testSelfDeadlockResourceTwice() {
        Trace trace1 = new Trace(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);

        trace1.delivered(NODE_1);

        trace1.suspendDeadlocked(new Object());

        runInterleavedSet(SETS, trace1);
    }

    @Test public void testSelfSuspendBeforeDeliver() {
        Trace trace1 = new Trace(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1);
    }

    @Test public void testSelfGrantBeforeSuspend() {
        Trace trace1 = new Trace(NODE_1);

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);
        trace1.granted(RES_A, NODE_1);

        trace1.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Two node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testDuoRequestResponseRequestersWaitsForResponse() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);

        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);

        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_1);
        trace2.sent(NODE_1);

        trace1.delivered(NODE_2);
        trace1.granted(RES_A, NODE_2);

        trace1.suspendNotDeadlocked();

        trace2.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1, trace2);
    }

    @Test public void testDuoRequestMultipleResponse() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);

        trace1.waitFor(RES_A, NODE_2);
        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);

        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_1);
        trace2.sent(NODE_1);

        trace1.delivered(NODE_2);
        trace1.granted(RES_A, NODE_2);

        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_1);
        trace2.sent(NODE_1);

        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);
        trace1.granted(RES_A, NODE_2);

        trace1.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1, trace2);
    }

    @Test public void testDuoMutualRequestResponse() {
        final Trace trace1 = new Trace(NODE_1);
        final Trace trace2 = new Trace(NODE_2);

        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);
        trace1.suspendNotDeadlocked();

        trace2.waitFor(RES_B, NODE_1);
        trace2.sent(NODE_1);
        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);
        trace1.sent(NODE_2);
        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_1);
        trace2.sent(NODE_1);
        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);
        trace1.granted(RES_A, NODE_2);

        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_1);
        trace2.granted(RES_B, NODE_1);

        trace2.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1, trace2);
    }

    @Test public void testDuoMutualRequestResponseDeadlocked() {
        final Trace trace1 = new Trace(NODE_1);
        final Trace trace2 = new Trace(NODE_2);
        final Object marker = new Object();

        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);
        trace1.suspendNotDeadlocked();

        trace2.waitFor(RES_B, NODE_1);
        trace2.sent(NODE_1);
        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);

        trace2.delivered(NODE_1);

        trace1.suspendDeadlocked(marker);

        trace2.suspendDeadlocked(marker);

        runInterleavedSet(SETS, trace1, trace2);
    }

    /**
     * In this scenario, node 1 is deadlocked, but can still answer node 2. But, node 2's request does not help node 1
     * to make progress, so it is still deadlocked after the exchange. In this case, deadlock can be reported twice.
     */
    @Test @Ignore public void testDuoRequestResponseDeadlockReportedTwice() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);

        Object marker1 = new Object();

        trace1.waitFor(RES_A, NODE_1);
        trace1.sent(NODE_1);

        trace1.delivered(NODE_1);

        trace1.suspendDeadlocked(marker1);

        trace2.waitFor(RES_B, NODE_1);
        trace2.sent(NODE_1);

        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);
        trace1.sent(NODE_2);

        trace1.suspendDeadlocked(marker1);

        trace2.delivered(NODE_1);
        trace2.granted(RES_B, NODE_1);

        trace2.suspendDeadlocked(new Object());

        runInterleavedSet(SETS, trace1, trace2);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Three node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testThreeNodeDeadlock() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);
        Trace trace3 = new Trace(NODE_3);
        Object marker = new Object();

        /* 1.i */
        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);

        /* 2.i */
        trace2.waitFor(RES_B, NODE_3);
        trace2.sent(NODE_3);

        /* 3.i */
        trace3.waitFor(RES_C, NODE_1);
        trace3.sent(NODE_1);

        /* 3.i */
        trace1.delivered(NODE_3);

        /* 1.i */
        trace2.delivered(NODE_1);

        /* 2.i */
        trace3.delivered(NODE_2);

        trace1.suspendDeadlocked(marker);

        trace3.suspendDeadlocked(marker);

        trace2.suspendDeadlocked(marker);

        runInterleavedSet(SETS, trace1, trace2, trace3);
    }

    @Test public void testThreeNodeChasing() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);
        Trace trace3 = new Trace(NODE_3);

        /* 1.i  */
        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);

        /* 2.i  */
        trace2.waitFor(RES_B, NODE_3);
        trace2.sent(NODE_3);

        /* 3.i  */
        trace3.waitFor(RES_C, NODE_1);
        /* 3.i  */
        trace3.sent(NODE_1);

        /* 3.i  */
        trace1.delivered(NODE_3);
        /* 1.ii */
        trace1.sent(NODE_3);

        /* 1.i  */
        trace2.delivered(NODE_1);
        /* 2.ii */
        trace2.sent(NODE_1);

        /* 2.i  */
        trace3.delivered(NODE_2);
        /* 3.ii */
        trace3.sent(NODE_2);

        /* 2.ii */
        trace1.delivered(NODE_2);
        /* 2.ii */
        trace1.granted(RES_A, NODE_2);

        /* 3.ii */
        trace2.delivered(NODE_3);
        /* 3.ii */
        trace2.granted(RES_A, NODE_3);

        /* 1.ii */
        trace3.delivered(NODE_1);
        /* 1.ii */
        trace3.granted(RES_A, NODE_1);

        trace1.suspendNotDeadlocked();

        trace2.suspendNotDeadlocked();

        trace3.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1, trace2, trace3);
    }

    @Test public void testThreeNodeTransitiveWait() {
        Trace trace1 = new Trace(NODE_1);
        Trace trace2 = new Trace(NODE_2);
        Trace trace3 = new Trace(NODE_3);

        trace2.waitFor(RES_A, NODE_3);
        trace2.sent(NODE_3);

        trace3.delivered(NODE_2);

        trace1.waitFor(RES_A, NODE_2);
        trace1.sent(NODE_2);

        trace2.delivered(NODE_1);
        trace2.suspendNotDeadlocked();

        trace3.sent(NODE_2);
        trace3.suspendNotDeadlocked();

        trace1.suspendNotDeadlocked();

        trace2.delivered(NODE_3);
        trace2.granted(RES_A, NODE_3);

        trace2.sent(NODE_1);
        trace2.suspendNotDeadlocked();

        trace1.delivered(NODE_2);
        trace1.granted(RES_A, NODE_2);

        trace1.suspendNotDeadlocked();

        runInterleavedSet(SETS, trace1, trace2, trace3);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Trace
    ///////////////////////////////////////////////////////////////////////////

    private static class Trace {

        private final Integer node;
        private final List<Step> steps;
        private MultiSet.Immutable<Object> markers;

        public Trace(Integer node) {
            this.node = node;
            this.steps = new ArrayList<>();
            this.markers = MultiSet.Immutable.of();
        }

        public Iterable<Step> steps() {
            return steps;
        }

        public MultiSet.Immutable<Object> markers() {
            return markers;
        }

        public void sent(Integer reciever) {
            steps.add((wfg, clock, markers, logger) -> {
                logger.apply(node + " sent to " + reciever);
                return clock.sent(reciever);
            });
        }

        public void delivered(Integer sender) {
            steps.add((wfg, clock, markers, logger) -> {
                logger.apply(node + " delivered from " + sender);
                return clock.delivered(sender);
            });
        }

        public void waitFor(String token, Integer other) {
            steps.add((wfg, clock, markers, logger) -> {
                logger.apply(node + " waits for " + other + "/" + token);
                wfg.waitFor(node, token, other);
                return clock;
            });
        }

        public void granted(String token, Integer other) {
            steps.add((wfg, clock, markers, logger) -> {
                logger.apply(node + " was granted " + other + "/" + token);
                wfg.granted(node, token, other);
                return clock;
            });
        }

        public void suspendNotDeadlocked() {
            steps.add((wfg, clock, markers, logger) -> {
                // FIXME these can be optional
                logger.apply(node + " suspended");
                final Deadlock<Integer, Unit, String> suspend = wfg.suspend(node, unit, clock);
                if(!suspend.isEmpty()) {
                    throw new AssertionError("Unexpected deadlock: " + suspend.edges());
                }
                return clock;
            });
        }

        public void suspendDeadlocked(Object marker) {
            markers = markers.add(marker);
            steps.add((wfg, clock, markers, logger) -> {
                logger.apply(node + " suspended");
                final Deadlock<Integer, Unit, String> suspend = wfg.suspend(node, unit, clock);
                if(!suspend.isEmpty()) {
                    if(!markers.contains(marker)) {
                        throw new AssertionError("Deadlock reported twice.");
                    } else {
                        markers.removeAll(marker);
                    }
                } else {
                    if(markers.contains(marker) && markers.remove(marker) == 0) {
                        throw new AssertionError("Expected deadlock");
                    }
                }
                return clock;
            });
        }

    }

    private interface Step {

        Clock<Integer> step(WaitForGraph<Integer, Unit, String> wfg, Clock<Integer> clock,
                MultiSet.Transient<Object> markers, Action1<String> logger);

    }

    ///////////////////////////////////////////////////////////////////////////
    // Exectute
    ///////////////////////////////////////////////////////////////////////////

    @SafeVarargs private final void runInterleaved(Trace... traces) {

        final LinkedList<LinkedList<Step>> nodes = Arrays.stream(traces).map(tr -> Lists.newLinkedList(tr.steps()))
                .filter(tr -> !tr.isEmpty()).collect(Collectors.toCollection(LinkedList::new));

        final MultiSet.Transient<Object> markers = MultiSet.Transient.of();
        Arrays.stream(traces).forEach(tr -> markers.addAll(tr.markers()));

        final LinkedList<Clock<Integer>> clocks =
                nodes.stream().map(tr -> Clock.<Integer>of()).collect(Collectors.toCollection(LinkedList::new));

        final WaitForGraph<Integer, Unit, String> wfg = new WaitForGraph<>();
        final List<String> log = Lists.newArrayList();

        final Random rnd = new Random();
        while(!nodes.isEmpty()) {
            final int idx = rnd.nextInt(nodes.size());

            final LinkedList<Step> steps = nodes.get(idx);
            final Clock<Integer> clock = clocks.get(idx);

            final Step step = steps.remove();
            try {
                Clock<Integer> newClock = step.step(wfg, clock, markers, log::add);
                clocks.set(idx, newClock);
            } catch(AssertionError ex) {
                throw new AssertionError(ex.getMessage() + "\n" + log.stream().collect(Collectors.joining("\n")));
            }

            if(steps.isEmpty()) {
                nodes.remove(idx);
                clocks.remove(idx);
            }
        }
    }

    @SafeVarargs private final void runInterleavedSet(int n, Trace... traces) {
        for(int i = 0; i < n; i++) {
            runInterleaved(traces);
        }
    }

}