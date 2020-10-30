package mb.statix.benchmarks.actors;

import java.util.concurrent.TimeUnit;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

import mb.statix.concurrent.actors.IActor;
import mb.statix.concurrent.actors.IActorRef;
import mb.statix.concurrent.actors.TypeTag;
import mb.statix.concurrent.actors.futures.CompletableFuture;
import mb.statix.concurrent.actors.futures.ICompletable;
import mb.statix.concurrent.actors.futures.ICompletableFuture;
import mb.statix.concurrent.actors.impl.ActorSystem;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Threads(1)
@State(Scope.Thread)
public class ProducerConsumerBenchmark {

    private static final ILogger logger = LoggerUtils.logger(ProducerConsumerBenchmark.class);

    @Param({ "1", "2", "4", "8" }) public int parallelism;
    @Param({ "0", "8", "32", "128" }) public int producers;
    @Param({ "1024", "8192", "65536", "262144" }) public int messages;

    @Benchmark public Object run() throws Exception, InterruptedException {
        return doRun(producers, messages, parallelism);
    }

    private static Integer doRun(int producers, int messages, int parallelism) throws Exception, InterruptedException {
        final ActorSystem actorSystem = new ActorSystem(parallelism);
        final ICompletableFuture<Integer> result = new CompletableFuture<>();
        final IActor<IConsumer> consumer = actorSystem.add("consumer", TypeTag.of(IConsumer.class),
                self -> new Consumer(self, producers * messages, result));
        for(int i = 0; i < producers; i++) {
            final IActor<IProducer> producer = actorSystem.add("producer-" + i, TypeTag.of(IProducer.class),
                    self -> new Producer(self, consumer, messages));
            actorSystem.async(producer).sentAll();
        }
        actorSystem.start();
        Integer received = result.get();
        actorSystem.stop();
        return received;
    }

    private interface IProducer {
        void sentAll();
    }

    private interface IConsumer {
        void receive();
    }

    private static class Producer implements IProducer {

        private final IActor<IProducer> self;
        private final IActorRef<IConsumer> consumer;
        private final int messages;

        public Producer(IActor<IProducer> self, IActorRef<IConsumer> consumer, int messages) {
            this.self = self;
            this.consumer = consumer;
            this.messages = messages;
        }

        @Override public void sentAll() {
            final IConsumer consumer = self.async(this.consumer);
            for(int i = 0; i < messages; i++) {
                consumer.receive();
            }
        }

    }

    private static class Consumer implements IConsumer {

        private final int expected;
        private final ICompletable<Integer> result;
        private int received = 0;

        public Consumer(IActor<IConsumer> self, int expected, ICompletable<Integer> result) {
            this.expected = expected;
            this.result = result;
            tryFinish();
        }

        @Override public void receive() {
            received++;
            tryFinish();
        }

        private void tryFinish() {
            if(received >= expected) {
                result.complete(received);
            }
        }

    }

    public static void main(String[] args) {
        try {
            while(true) {
                doRun(0, 1024, 1);
            }
        } catch(Exception ex) {
            logger.error("Failed.", ex);
        }
    }

}