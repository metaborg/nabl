package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class ActorTest {

    private static final ILogger logger = LoggerUtils.logger(ActorTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        final IActorRef<IPong> ponger = system.add("ponger", TypeTag.of(IPong.class), self -> new Pong(self));
        final IActorRef<IPing> pinger = system.add("pinger", TypeTag.of(IPing.class), self -> new Ping(self, ponger));
        system.start();
        pinger.get().start();
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
            ponger.get().ping(self);

        }

        @Override public void pong() {
            logger.info("pong");
            self.stop();
        }

    }

    private static interface IPong {
        void ping(IActorRef<IPing> pinger);
    }

    private static class Pong implements IPong {

        final IActor<IPong> self;

        public Pong(IActor<IPong> self) {
            this.self = self;
        }

        @Override public void ping(IActorRef<IPing> pinger) {
            logger.info("ping");
            pinger.get().pong();
            self.stop();
        }

    }

}