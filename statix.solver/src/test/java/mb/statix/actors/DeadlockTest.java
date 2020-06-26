package mb.statix.actors;

import java.util.concurrent.ExecutionException;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import io.usethesource.capsule.SetMultimap;

public class DeadlockTest {

    private static final ILogger logger = LoggerUtils.logger(DeadlockTest.class);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ActorSystem system = new ActorSystem();
        TypeTag<IDeadlockDetector<Object>> tt = TypeTag.of(IDeadlockDetector.class);
        final IActor<IDeadlockDetector<Object>> dlm = system.add("dlm", tt, self -> new DeadlockDetector<>(self));
        final IActor<IPingPong> pp1 = system.add("pp1", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        pp1.addMonitor(dlm);
        //        final IActor<IPingPong> pp2 = system.add("pp2", TypeTag.of(IPingPong.class), self -> new PingPong(self, dlm));
        //        pp2.addMonitor(dlm);
        system.start();
        pp1.get().start(pp1);
        //        pp1.get().start(pp2);
        //        pp2.get().start(pp1);
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
        private final IActorRef<IDeadlockDetector<Object>> dlm;

        public PingPong(IActor<IPingPong> self, IActorRef<IDeadlockDetector<Object>> dlm) {
            this.self = self;
            this.dlm = dlm;
        }

        @Override public void start(IActorRef<? extends IPing> target) {
            logger.info("start");
            target.get().ping(self);
            dlm.get().waitFor(self, "pong", target);
        }

        @Override public void ping(IActorRef<? extends IPong> source) {
            logger.info("ping");
            //            source.get().pong(self);

        }

        @Override public void pong(IActorRef<? extends IPong> source) {
            logger.info("pong");
            dlm.get().granted(self, "pong", source);
            self.stop();
        }

        @Override public void deadlocked(SetMultimap<IActorRef<?>, Object> waitFors) {
            logger.error("detected deadlock: {}", waitFors);
        }

    }

    ///////////////////////////////////////////////////////////////////
    // DeadlockDetector
    ///////////////////////////////////////////////////////////////////

    private static interface CanDeadlock<T> {

        void deadlocked(SetMultimap<IActorRef<?>, T> waitFors);

    }

    private static interface IDeadlockDetector<T> extends IActorMonitor {

        void waitFor(IActorRef<?> source, T token, IActorRef<?> target);

        void granted(IActorRef<?> source, T token, IActorRef<?> target);

    }

    private static class DeadlockDetector<T> implements IDeadlockDetector<T> {

        private final IActor<IDeadlockDetector<T>> self;

        private final WaitForGraph<IActorRef<?>, T> wfg = new WaitForGraph<>();

        public DeadlockDetector(IActor<IDeadlockDetector<T>> self) {
            this.self = self;
        }

        @Override public void waitFor(IActorRef<?> source, T token, IActorRef<?> target) {
            if(!(source.get() instanceof CanDeadlock)) {
                throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
            }
            if(!(target.get() instanceof CanDeadlock)) {
                throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
            }
            wfg.waitFor(source, token, target);
        }

        @Override public void granted(IActorRef<?> source, T token, IActorRef<?> target) {
            if(!(source.get() instanceof CanDeadlock)) {
                throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
            }
            if(!(target.get() instanceof CanDeadlock)) {
                throw new IllegalArgumentException("Actor " + source + " must implement CanDeadlock");
            }
            wfg.granted(source, token, target);
        }

        @SuppressWarnings("unchecked") @Override public void suspend(IActorRef<?> actor) {
            if(!(actor.get() instanceof CanDeadlock)) {
                return;
            }
            final SetMultimap<IActorRef<?>, T> waitFors = wfg.suspend(actor);
            if(waitFors.isEmpty()) {
                return;
            }
            ((CanDeadlock<T>) actor.get()).deadlocked(waitFors);
        }

        @Override public void resume(IActorRef<?> actor) {
            if(!(actor.get() instanceof CanDeadlock)) {
                return;
            }
            wfg.activate(actor);
        }

    }

}