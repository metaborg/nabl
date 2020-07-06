package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.impl.ActorSystem;

public class ActorTest {

    private static final ILogger logger = LoggerUtils.logger(ActorTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        final IActorRef<IPong> ponger = system.add("ponger", TypeTag.of(IPong.class), self -> new Pong(self));
        final IActorRef<IPing> pinger = system.add("pinger", TypeTag.of(IPing.class), self -> new Ping(self, ponger));
        system.start();
        system.async(pinger).start();
        Thread.sleep(2000);
        system.stop();
    }

    interface IPing {

        void start();

        void pong();

    }

    private static class Ping implements IPing {

        final IActor<IPing> self;
        final IActorRef<IPong> ponger;

        public Ping(IActor<IPing> self, IActorRef<IPong> ponger) {
            this.self = self;
            this.ponger = ponger;
        }

        @Override public void start() {
            logger.info("start");
            self.async(ponger).ping();

        }

        @Override public void pong() {
            logger.info("pong");
            self.stop();
        }

    }

    private static interface IPong {
        void ping();
    }

    private static class Pong implements IPong {

        final IActor<IPong> self;

        public Pong(IActor<IPong> self) {
            this.self = self;
        }

        @Override public void ping() {
            logger.info("ping");
            self.async((IActorRef<IPing>) self.sender()).pong();
            self.stop();
        }

    }

}