package mb.statix.actors.deadlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.metaborg.util.unit.Unit.unit;

import java.util.Optional;

import org.junit.Test;
import org.metaborg.util.unit.Unit;

import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.deadlock.Deadlock;
import mb.statix.concurrent.actors.deadlock.WaitForGraph;

// @formatter:off
public class WaitForGraphTest {

    private static final String NODE_1 = "1";
    private static final String NODE_2 = "2";
    private static final String NODE_3 = "3";

    private static final String RES_A = "A";
    private static final String RES_B = "B";
    private static final String RES_C = "C";

    ///////////////////////////////////////////////////////////////////////////
    // Single node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testSelfDeadlock() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        clock1 = clock1.delivered(NODE_1);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 1);
    }

    @Test public void testSelfDeadlockTwoResources() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        wfg.waitFor(NODE_1, RES_B, NODE_1);
        clock1 = clock1.sent(NODE_1);

        clock1 = clock1.delivered(NODE_1);

        clock1 = clock1.delivered(NODE_1);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 2);
    }

    @Test public void testSelfDeadlockResourceTwice() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        clock1 = clock1.delivered(NODE_1);

        clock1 = clock1.delivered(NODE_1);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 2);
    }

    @Test public void testSelfSuspendBeforeDeliver() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        assertActive(wfg.suspend(NODE_1, unit, clock1));
    }

    @Test public void testSelfGrantBeforeSuspend() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_1);
        clock1 = clock1.sent(NODE_1);

        clock1 = clock1.delivered(NODE_1);
        clock1 = clock1.sent(NODE_1);

        clock1 = clock1.delivered(NODE_1);
        wfg.granted(NODE_1, RES_A, NODE_1);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Two node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testDuoRequestResponseBeforeSuspends() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);
    }

    @Test public void testDuoRequestResponseRequestersWaitsForResponse() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);
    }

    @Test public void testDuoRequestResponseRequestersWaitsAfterResponse() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

        assertActive(wfg.suspend(NODE_1, unit, clock1));

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);
    }

    @Test public void testDuoRequestMultipleResponse() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_2);
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);
    }

    @Test public void testDuoRequestResponseResponderSuspendsAfterRequest() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                               assertActive(wfg.suspend(NODE_2, unit, clock2));

                                             clock2 = clock2.delivered(NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Assertions
    ///////////////////////////////////////////////////////////////////////////

    private void assertActive(Optional<Optional<Deadlock<String, Unit, String>>> suspend) {
        if(suspend.isPresent()) {
            throw new AssertionError("Expected active, but got " + (suspend.get().isPresent() ? "deadlocked" : "waiting"));
        }
    }

    private void assertWaiting(Optional<Optional<Deadlock<String, Unit, String>>> suspend) {
        assertTrue("Expected waiting, but got active", suspend.isPresent());
        assertFalse("Expected waiting, but got deadlocked", suspend.get().isPresent());
    }

    private void assertDeadlock(Optional<Optional<Deadlock<String, Unit, String>>> suspend, int expectedNodes,
            int expectedWaitFors) {
        assertTrue("Expected deadlocked, but got active", suspend.isPresent());
        assertTrue("Expected deadlock, but got waiting", suspend.get().isPresent());
        assertEquals(expectedNodes, suspend.get().get().nodes().size());
        assertEquals(expectedWaitFors, suspend.get().get().edges().size());
    }

}
// @formatter:on