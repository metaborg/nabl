package mb.statix.concurrent.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;

public class LocalTest {

    private static final ILogger logger = LoggerUtils.logger(LocalTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        system.add("local", TypeTag.of(ILocal.class), self -> new Test(self));
        system.start();
        Thread.sleep(2000);
        system.stop();
    }

    interface ILocal {

        IFuture<String> get();

    }

    private static class Test implements ILocal {

        final IActor<ILocal> self;

        public Test(IActor<ILocal> self) {
            this.self = self;
            self.async(self).get().whenComplete((r, ex) -> {
                logger.info("We said: {}", r);
            });
        }

        @Override public IFuture<String> get() {
            return CompletableFuture.completedFuture("Hello!");
        }

    }

}