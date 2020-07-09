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

    @Test public void testDuoMutualRequestResponseDeadlock() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
                                                  
                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 2, 2);
    }

    @Test public void testDuoMutualRequestResponseSuspendAtTheEnd() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
        /* A.2 */
        clock1 = clock1.sent(NODE_2);
                                                  
                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);
                                                                      /* B.2 */ 
                                                  clock2 = clock2.sent(NODE_1);

        /* B.2 */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

                                                                      /* A.2 */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

    }

    @Test public void testDuoMutualRequestResponseSuspendActivatesOther() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        /* 1.i */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                                                                      /* 2.i */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                                                                      /* 1.i */
                                             clock2 = clock2.delivered(NODE_1);
                                                                     /* 2.ii */ 
                                                  clock2 = clock2.sent(NODE_1);

                              assertWaiting(wfg.suspend(NODE_2, unit, clock2));
                              assertFalse(wfg.isWaiting(NODE_1));
                              // at this point, 1 and two have mutual wait-fors,
                              // but 1 is activated here, so no deadlock is
                              // reported
                                                  
        /* 2.i */
        clock1 = clock1.delivered(NODE_2);
        /* 1.ii */
        clock1 = clock1.sent(NODE_2);
                                                  
        assertActive(wfg.suspend(NODE_1, unit, clock1));
        assertFalse(wfg.isWaiting(NODE_2));
        // at this point, 1 and two still have mutual
        // wait-fors, but now 2 is also activated, and
        // 1 remains active because 2.ii is not delivered

                                                                     /* 1.ii */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

        /* 2.ii */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

    }

    ///////////////////////////////////////////////////////////////////////////
    // Interleavings of two node mutual request-reponse scenario
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testDuoMutualRequestResponseInterleaving1() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
        /* A.2 */
        clock1 = clock1.sent(NODE_2);
                                                  
        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.2 */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);
                                                                      /* B.2 */ 
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

                                                                      /* A.2 */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

    }

    @Test public void testDuoMutualRequestResponseInterleaving2() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
        /* A.2 */
        clock1 = clock1.sent(NODE_2);
                                                  
        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.2 */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);
                                                                      /* B.2 */ 
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

                                                                      /* A.2 */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

    }

    @Test public void testDuoMutualRequestResponseInterleaving3() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);
                                                                      /* B.2 */ 
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

                                                                      /* A.2 */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
        /* A.2 */
        clock1 = clock1.sent(NODE_2);
                                                  
        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.2 */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

    }

    @Test public void testDuoMutualRequestResponseInterleaving4() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();

                                                                      /* B.1 */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_1);
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

                                                                      /* A.1 */
                                             clock2 = clock2.delivered(NODE_1);
                                                                      /* B.2 */ 
                                                  clock2 = clock2.sent(NODE_1);

                        assertNotDeadlocked(wfg.suspend(NODE_2, unit, clock2));

        /* A.1 */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.1 */
        clock1 = clock1.delivered(NODE_2);
        /* A.2 */
        clock1 = clock1.sent(NODE_2);
                                                  
        assertNotDeadlocked(wfg.suspend(NODE_1, unit, clock1));

        /* B.2 */
        clock1 = clock1.delivered(NODE_2);
        wfg.granted(NODE_1, RES_A, NODE_2);

        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                                                                      /* A.2 */
                                             clock2 = clock2.delivered(NODE_1);
                                            wfg.granted(NODE_2, RES_B, NODE_1);

                       assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

    }

    ///////////////////////////////////////////////////////////////////////////
    // Three node scenarios
    ///////////////////////////////////////////////////////////////////////////

    @Test public void testThreeNodeDeadlock() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();
        Clock<String> clock3 = Clock.of();

        /* 1.i */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                                         /* 2.i */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_3);
                                               clock2 = clock2.sent(NODE_3);

                                                                                                         /* 3.i */ 
                                                                               wfg.waitFor(NODE_3, RES_C, NODE_1);
                                                                                     clock3 = clock3.sent(NODE_1);

        /* 3.i */
        clock1 = clock1.delivered(NODE_3);
                                                                                     
                                                       /* 1.i */
                                          clock2 = clock2.delivered(NODE_1);
                                                                                     
                                                                                                         /* 2.i */
                                                                                clock3 = clock3.delivered(NODE_2);
                                                                                     
        assertWaiting(wfg.suspend(NODE_1, unit, clock1));

                                                                 assertWaiting(wfg.suspend(NODE_3, unit, clock3));
                                                  
                                   assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 3, 3);

    }

    @Test public void testThreeNodeChasingSuspendAtTheEnd() {
        final WaitForGraph<String, Unit, String> wfg = new WaitForGraph<>();
        Clock<String> clock1 = Clock.of();
        Clock<String> clock2 = Clock.of();
        Clock<String> clock3 = Clock.of();

        /* 1.i  */
        wfg.waitFor(NODE_1, RES_A, NODE_2);
        clock1 = clock1.sent(NODE_2);

                                                       /* 2.i  */ 
                                            wfg.waitFor(NODE_2, RES_B, NODE_3);
                                               clock2 = clock2.sent(NODE_3);

                                                                                                        /* 3.i  */
                                                                               wfg.waitFor(NODE_3, RES_C, NODE_1);
                                                                                                        /* 3.i  */
                                                                                     clock3 = clock3.sent(NODE_1);

        /* 3.i  */
        clock1 = clock1.delivered(NODE_3);
        /* 1.ii */
        clock1 = clock1.sent(NODE_3);
                                                                                     
                                                       /* 1.i  */
                                          clock2 = clock2.delivered(NODE_1);
                                                       /* 2.ii */
                                             clock2 = clock2.sent(NODE_1);
                                                                                     
                                                                                                        /* 2.i  */
                                                                                clock3 = clock3.delivered(NODE_2);
                                                                                                        /* 3.ii */
                                                                                     clock3 = clock3.sent(NODE_2);
                                                                                     
        /* 2.ii */
        clock1 = clock1.delivered(NODE_2);
        /* 2.ii */
        wfg.granted(NODE_1, RES_A, NODE_2);

                                                       /* 3.ii */
                                           clock2 = clock2.delivered(NODE_3);
                                                       /* 3.ii */
                                           wfg.granted(NODE_2, RES_A, NODE_3);

                                                                                                        /* 1.ii */
                                                                                clock3 = clock3.delivered(NODE_1);
                                                                                                        /* 1.ii */
                                                                               wfg.granted(NODE_3, RES_A, NODE_1);
                                                                                     
        assertDeadlock(wfg.suspend(NODE_1, unit, clock1), 1, 0);

                                   assertDeadlock(wfg.suspend(NODE_2, unit, clock2), 1, 0);

                                                                 assertDeadlock(wfg.suspend(NODE_3, unit, clock3), 1, 0);
                                                  
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

    private void assertNotDeadlocked(Optional<Optional<Deadlock<String, Unit, String>>> suspend) {
        if(suspend.isPresent() && suspend.get().isPresent()) {
            throw new AssertionError("Expected not deadlocked, but is deadlocked");
        }
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