package mb.p_raffrayi.actors;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.metaborg.util.future.CompletableFuture;
import org.metaborg.util.future.ICompletableFuture;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.unit.Unit;

import mb.p_raffrayi.actors.impl.ActorSystem;

public class ActorTest {

    private static final TypeTag<EmptyActor> EMPTY_ACTOR = TypeTag.of(EmptyActor.class);
    private static final TypeTag<PingActor> PING_ACTOR = TypeTag.of(PingActor.class);

    @Test(timeout = 10_000) public void testNoActors() throws InterruptedException, ExecutionException {
        final IActorSystem system = new ActorSystem();
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testOneActorNoop()
            throws InterruptedException, ExecutionException, TimeoutException {
        final ICompletableFuture<Unit> oneStopped = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void stopped(Throwable ex) {
                oneStopped.complete(Unit.unit);
            }

        });
        system.stop().asJavaCompletion().get();
        oneStopped.asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testOneActorSupplierFailed() throws InterruptedException, ExecutionException {
        final IActorSystem system = new ActorSystem();
        system.add("one", EMPTY_ACTOR, (self) -> {
            throw new RuntimeException();
        });
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testOneActorStartMonitorFails()
            throws InterruptedException, ExecutionException {
        final ICompletableFuture<Unit> oneFailed = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                throw new RuntimeException();
            };

            @Override public void stopped(Throwable ex) {
                oneFailed.complete(Unit.unit);
            }

        });
        oneFailed.asJavaCompletion().get();
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testCreateChildInStartEvent() throws InterruptedException, ExecutionException {
        final IActorSystem system = new ActorSystem();
        system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                self.add("two", EMPTY_ACTOR, (self) -> new EmptyActor() {});
            }

        });
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testChildCrashStopsParent() throws InterruptedException, ExecutionException {
        final ICompletableFuture<Unit> oneStopped = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                self.add("two", EMPTY_ACTOR, (self) -> new EmptyActor() {

                    @Override public void started() {
                        throw new RuntimeException();
                    }

                });
            };

            @Override public void stopped(Throwable ex) {
                oneStopped.complete(Unit.unit);
            }

        });
        oneStopped.asJavaCompletion().get();
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testPingFromSystem() throws InterruptedException, ExecutionException {
        final IActorSystem system = new ActorSystem();
        final IActorRef<PingActor> one = system.add("one", PING_ACTOR, (self) -> new PingActor() {

            @Override public IFuture<Unit> ping() {
                return CompletableFuture.completedFuture(Unit.unit);
            };

        });
        final IFuture<Unit> pong = system.async(one).ping();
        pong.asJavaCompletion().get();
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testPingFromParent() throws InterruptedException, ExecutionException {
        final ICompletableFuture<Unit> oneGotPong = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        final IActorRef<EmptyActor> one = system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                final IActorRef<PingActor> two = self.add("two", PING_ACTOR, (self) -> new PingActor() {

                    @Override public IFuture<Unit> ping() {
                        return CompletableFuture.completedFuture(Unit.unit);
                    }

                });
                final IFuture<Unit> pong = self.async(two).ping();
                pong.thenAccept((u) -> {
                    oneGotPong.complete(Unit.unit);
                });
            }

        });
        oneGotPong.asJavaCompletion().get();
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testActorsStopInOrder0() throws InterruptedException, ExecutionException {
        final AtomicInteger order = new AtomicInteger();
        final ICompletableFuture<Integer> oneStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> twoStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> threeStopped = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        IActorRef<EmptyActor> one = system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                IActorRef<EmptyActor> two = self.add("two", EMPTY_ACTOR, (self) -> new EmptyActor() {

                    @Override public void started() {
                        IActorRef<EmptyActor> three = self.add("three", EMPTY_ACTOR, (self) -> new EmptyActor() {

                            @Override public void started() {
                            }

                            @Override public void stopped(Throwable ex) {
                                threeStopped.complete(order.getAndIncrement());
                            }

                        });
                    }

                    @Override public void stopped(Throwable ex) {
                        twoStopped.complete(order.getAndIncrement());
                    }

                });
                throw new RuntimeException();
            };

            @Override public void stopped(Throwable ex) {
                oneStopped.complete(order.getAndIncrement());
            }

        });
        assertEquals(0, (int) threeStopped.asJavaCompletion().get());
        assertEquals(1, (int) twoStopped.asJavaCompletion().get());
        assertEquals(2, (int) oneStopped.asJavaCompletion().get());
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testActorsStopInOrder1() throws InterruptedException, ExecutionException {
        final AtomicInteger order = new AtomicInteger();
        final ICompletableFuture<Integer> oneStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> twoStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> threeStopped = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        IActorRef<EmptyActor> one = system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                IActorRef<EmptyActor> two = self.add("two", EMPTY_ACTOR, (self) -> new EmptyActor() {

                    @Override public void started() {
                        IActorRef<EmptyActor> three = self.add("three", EMPTY_ACTOR, (self) -> new EmptyActor() {

                            @Override public void started() {
                            }

                            @Override public void stopped(Throwable ex) {
                                threeStopped.complete(order.getAndIncrement());
                            }

                        });
                        throw new RuntimeException();
                    }

                    @Override public void stopped(Throwable ex) {
                        twoStopped.complete(order.getAndIncrement());
                    }

                });
            };

            @Override public void stopped(Throwable ex) {
                oneStopped.complete(order.getAndIncrement());
            }

        });
        assertEquals(0, (int) threeStopped.asJavaCompletion().get());
        assertEquals(1, (int) twoStopped.asJavaCompletion().get());
        assertEquals(2, (int) oneStopped.asJavaCompletion().get());
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testActorsStopInOrder2() throws InterruptedException, ExecutionException {
        final AtomicInteger order = new AtomicInteger();
        final ICompletableFuture<Integer> oneStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> twoStopped = new CompletableFuture<>();
        final ICompletableFuture<Integer> threeStopped = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        IActorRef<EmptyActor> one = system.add("one", EMPTY_ACTOR, (self) -> new EmptyActor() {

            @Override public void started() {
                IActorRef<EmptyActor> two = self.add("two", EMPTY_ACTOR, (self) -> new EmptyActor() {

                    @Override public void started() {
                        IActorRef<EmptyActor> three = self.add("three", EMPTY_ACTOR, (self) -> new EmptyActor() {

                            @Override public void started() {
                                throw new RuntimeException();
                            }

                            @Override public void stopped(Throwable ex) {
                                threeStopped.complete(order.getAndIncrement());
                            }

                        });
                    }

                    @Override public void stopped(Throwable ex) {
                        twoStopped.complete(order.getAndIncrement());
                    }

                });
            };

            @Override public void stopped(Throwable ex) {
                oneStopped.complete(order.getAndIncrement());
            }

        });
        assertEquals(0, (int) threeStopped.asJavaCompletion().get());
        assertEquals(1, (int) twoStopped.asJavaCompletion().get());
        assertEquals(2, (int) oneStopped.asJavaCompletion().get());
        system.stop().asJavaCompletion().get();
    }

    @Test(timeout = 10_000) public void testOneActorSelfResume() throws InterruptedException, ExecutionException {
        final ICompletableFuture<Unit> oneResumed = new CompletableFuture<>();
        final IActorSystem system = new ActorSystem();
        system.add("one", PING_ACTOR, (self) -> new PingActor() {

            private volatile boolean pinged = false;

            @Override public IFuture<Unit> ping() {
                pinged = true;
                return CompletableFuture.completedFuture(Unit.unit);
            }

            @Override public void resumed() {
                oneResumed.complete(Unit.unit);
            };

            @Override public void suspended() {
                if(!pinged) {
                    self.local().ping();
                }
            }

        });
        oneResumed.asJavaCompletion().get();
        system.stop().asJavaCompletion().get();
    }


    ///////////////////////////////////////////////////////////////////////////
    // Actor Interfaces
    ///////////////////////////////////////////////////////////////////////////

    private interface EmptyActor extends IActorMonitor {

    }

    private interface PingActor extends IActorMonitor {

        IFuture<Unit> ping();

    }

}