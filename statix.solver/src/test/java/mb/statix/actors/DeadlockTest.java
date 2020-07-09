package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.functions.Action2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorMonitor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.deadlock.Clock;
import mb.statix.concurrent.actors.deadlock.Deadlock;
import mb.statix.concurrent.actors.deadlock.DeadlockMonitor;
import mb.statix.concurrent.actors.deadlock.IDeadlockMonitor;
import mb.statix.concurrent.actors.impl.ActorSystem;

public class DeadlockTest {

    private static final ILogger logger = LoggerUtils.logger(DeadlockTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        TypeTag<IDeadlockMonitor<IPingPong, Void, String>> tt = TypeTag.of(IDeadlockMonitor.class);
        final IActor<IDeadlockMonitor<IPingPong, Void, String>> dlm =
                system.add("dlm", tt, self -> new DeadlockMonitor<>(self, dlh));
        final IActor<IPingPong> pp1 = system.add("pp1", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        final IActor<IPingPong> pp2 = system.add("pp2", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        system.start();
        //        system.async(pp1).start(pp1);
        system.async(pp1).start(pp2);
        system.async(pp2).start(pp1);
    }

    interface IPingPong {

        static TypeTag<IPingPong> TYPE = TypeTag.of(IPingPong.class);

        void start(IActorRef<? extends IPingPong> target);

        void ping();

        void pong();

    }

    private static class PingPong implements IPingPong, IActorMonitor {

        private final IActor<IPingPong> self;
        private final IActorRef<IDeadlockMonitor<IPingPong, Void, String>> dlm;

        private Clock<IActorRef<? extends IPingPong>> clock;

        public PingPong(IActor<IPingPong> self, IActorRef<IDeadlockMonitor<IPingPong, Void, String>> dlm) {
            this.self = self;
            this.dlm = dlm;

            this.clock = Clock.of();
            self.addMonitor(this);
        }

        @Override public void start(IActorRef<? extends IPingPong> target) {
            logger.info("{} started", self);
            self.async(target).ping();
            clock = clock.sent(target);
            self.async(dlm).waitFor(target, "pong");
        }

        @Override public void ping() {
            logger.info("{} recieved ping from {}", self, self.sender());
            clock = clock.delivered(self.sender(IPingPong.TYPE));
            //            clock = clock.sent(self.sender());
            //            self.async((IActorRef<IPong>)self.sender()).pong();

        }

        @Override public void pong() {
            logger.info("{} recieved pong from {}", self, self.sender());
            clock = clock.delivered(self.sender(IPingPong.TYPE));
            self.async(dlm).granted(self.sender(IPingPong.TYPE), "pong");
            self.stop();
        }

        @Override public void suspended(IActor<?> self) {
            self.async(dlm).suspended(null, clock);
        }

        @Override public void stopped(IActor<?> self) {
            self.async(dlm).stopped(clock);
        }

    }

    private static Action2<IActor<?>, Deadlock<IActorRef<? extends IPingPong>, Void, String>> dlh =
            (self, deadlock) -> {
                logger.error("{} detected deadlock: {}", self, deadlock);
                //        for(Entry<IActorRef<?>, String> waitFor : waitFors.entrySet()) {
                //            // reply after all
                //            self.async((IActorRef<? extends IPong>) waitFor.getKey()).pong(self);
                //        }
            };

}