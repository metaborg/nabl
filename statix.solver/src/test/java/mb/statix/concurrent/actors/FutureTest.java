package mb.statix.concurrent.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.IFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;

public class FutureTest {

    private static final ILogger logger = LoggerUtils.logger(FutureTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        final IActorRef<IServer> server = system.add("server", TypeTag.of(IServer.class), self -> new Server(self));
        final IActorRef<IClient> client =
                system.add("client", TypeTag.of(IClient.class), self -> new Client(self, server));
        system.start();
        Thread.sleep(2000);
        system.stop();
    }

    interface IClient {

    }

    interface IServer {

        IFuture<String> get();

    }

    private static class Client implements IClient {

        final IActor<IClient> self;

        public Client(IActor<IClient> self, IActorRef<IServer> server) {
            this.self = self;
            self.async(server).get().whenComplete((msg, ex) -> {
                logger.info("{} said: {}", server, msg);
            });
        }

    }

    private static class Server implements IServer {

        final IActor<IServer> self;

        public Server(IActor<IServer> self) {
            this.self = self;
        }

        @Override public IFuture<String> get() {
            return CompletableFuture.completedFuture("Hello!");
        }

    }

}