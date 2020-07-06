package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.nabl2.util.collections.IRelation3;
import mb.statix.actors.deadlock.Clock;
import mb.statix.actors.deadlock.DeadlockMonitor;
import mb.statix.actors.deadlock.IDeadlockMonitor;
import mb.statix.actors.impl.ActorSystem;

public class DeadlockTest {

    private static final ILogger logger = LoggerUtils.logger(DeadlockTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        TypeTag<IDeadlockMonitor<String>> tt = TypeTag.of(IDeadlockMonitor.class);
        final IActor<IDeadlockMonitor<String>> dlm = system.add("dlm", tt, self -> new DeadlockMonitor<>(self, dlh));
        final IActor<IPingPong> pp1 = system.add("pp1", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        final IActor<IPingPong> pp2 = system.add("pp2", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        system.start();
        //        system.async(pp1).start(pp1);
        system.async(pp1).start(pp2);
        system.async(pp2).start(pp1);
    }

    interface IPing {

        void ping();

    }

    interface IPong {

        void pong();

    }

    interface IPingPong extends IPing, IPong {

        void start(IActorRef<? extends IPing> target);

    }

    private static class PingPong implements IPingPong, IActorMonitor {

        private final IActor<IPingPong> self;
        private final IActorRef<IDeadlockMonitor<String>> dlm;

        private Clock clock;

        public PingPong(IActor<IPingPong> self, IActorRef<IDeadlockMonitor<String>> dlm) {
            this.self = self;
            this.dlm = dlm;

            this.clock = Clock.of();
            self.addMonitor(this);
        }

        @Override public void start(IActorRef<? extends IPing> target) {
            logger.info("{} started", self);
            self.async(target).ping();
            clock = clock.sent(target);
            self.async(dlm).waitFor(target, "pong");
        }

        @Override public void ping() {
            logger.info("{} received ping from {}", self, self.sender());
            clock = clock.received(self.sender());
            //            clock = clock.sent(self.sender());
            //            self.async((IActorRef<IPong>)self.sender()).pong();

        }

        @Override public void pong() {
            logger.info("{} recieved pong from {}", self, self.sender());
            clock = clock.received(self.sender());
            self.async(dlm).granted(self.sender(), "pong");
            self.stop();
        }

        @Override public void suspended(IActor<?> self) {
            self.async(dlm).suspended(clock);
        }

        @Override public void stopped(IActor<?> self) {
            self.async(dlm).stopped(clock);
        }

    }

    private static Action2<IActor<?>, IRelation3.Immutable<IActorRef<?>, String, IActorRef<?>>> dlh =
            (self, waitFors) -> {
                logger.error("{} detected deadlock: {}", self, waitFors);
                //        for(Entry<IActorRef<?>, String> waitFor : waitFors.entrySet()) {
                //            // reply after all
                //            self.async((IActorRef<? extends IPong>) waitFor.getKey()).pong(self);
                //        }
            };

}