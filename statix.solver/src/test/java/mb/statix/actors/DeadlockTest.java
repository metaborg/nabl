package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.SetMultimap;
import mb.statix.actors.deadlock.CanDeadlock;
import mb.statix.actors.deadlock.DeadlockMonitor;
import mb.statix.actors.deadlock.IDeadlockMonitor;

public class DeadlockTest {

    private static final ILogger logger = LoggerUtils.logger(DeadlockTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        TypeTag<IDeadlockMonitor<Object>> tt = TypeTag.of(IDeadlockMonitor.class);
        final IActor<IDeadlockMonitor<Object>> dlm = system.add("dlm", tt, self -> new DeadlockMonitor<>(self));
        final IActor<IPingPong> pp1 = system.add("pp1", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        pp1.addMonitor(dlm);
        final IActor<IPingPong> pp2 = system.add("pp2", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        pp2.addMonitor(dlm);
        system.start();
        //        pp1.get().start(pp1);
        pp1.async().start(pp2);
        pp2.async().start(pp1);
    }

    interface IPing {

        void ping(IActorRef<? extends IPong> source);

    }

    interface IPong {

        void pong(IActorRef<? extends IPong> source);

    }

    interface IPingPong extends IPing, IPong, CanDeadlock<Object> {

        void start(IActorRef<? extends IPing> target);

    }

    private static class PingPong implements IPingPong {

        private final IActor<IPingPong> self;
        private final IActorRef<IDeadlockMonitor<Object>> dlm;

        public PingPong(IActor<IPingPong> self, IActorRef<IDeadlockMonitor<Object>> dlm) {
            this.self = self;
            this.dlm = dlm;
        }

        @Override public void start(IActorRef<? extends IPing> target) {
            logger.info("start");
            target.async().ping(self);
            dlm.async().waitFor(self, "pong", target);
        }

        @Override public void ping(IActorRef<? extends IPong> source) {
            logger.info("ping");
            //            source.get().pong(self);

        }

        @Override public void pong(IActorRef<? extends IPong> source) {
            logger.info("pong");
            dlm.async().granted(self, "pong", source);
            self.stop();
        }

        @Override public void deadlocked(SetMultimap<IActorRef<?>, Object> waitFors) {
            logger.error("detected deadlock: {}", waitFors);
        }

    }

}