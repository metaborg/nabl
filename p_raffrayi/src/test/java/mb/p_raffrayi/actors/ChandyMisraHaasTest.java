package mb.p_raffrayi.actors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.junit.Test;
import org.metaborg.util.RefBool;
import org.metaborg.util.functions.Action0;
import org.metaborg.util.functions.Action2;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import mb.p_raffrayi.actors.deadlock.ChandyMisraHaas;

public class ChandyMisraHaasTest {

    private static class Process implements ChandyMisraHaas.Host<Process> {

        private final String name;

        private final Set<Process> dependentSet = new HashSet<>();

        private final Map<Process, Queue<Action0>> messages = new HashMap<>();

        private final ChandyMisraHaas<Process> cmh;

        public Process(String name, Action2<Process, Set<Process>> handleDeadlock) {
            this.name = name;
            this.cmh = new ChandyMisraHaas<Process>(this, nodes -> handleDeadlock.apply(this, nodes));
        }

        @Override public Process process() {
            return this;
        }

        @Override public Set<Process> dependentSet() {
            return dependentSet;
        }

        @Override public void query(Process k, Process i, int m) {
            k.enqueue(this, () -> k.cmh.query(i, m, this));
        }

        @Override public void reply(Process k, Process i, int m, Set<Process> R) {
            k.enqueue(this, () -> k.cmh.reply(i, m, R, this));
        }

        public void setDependencies(Process... processes) {
            for(Process process : processes) {
                dependentSet.add(process);
            }
        }

        public void enqueue(Process sender, Action0 message) {
            getQueue(sender).add(message);
        }

        public void step(Process sender) {
            final Queue<Action0> queue = getQueue(sender);
            queue.remove().apply();
            if(queue.isEmpty()) {
                messages.remove(sender);
            }
        }

        private Queue<Action0> getQueue(Process sender) {
            return messages.computeIfAbsent(sender, __ -> new LinkedList<>());
        }

        public boolean processAll() {
            final RefBool result = new RefBool();
            boolean progress;
            do {
                progress = false;
                for(Process process : new HashSet<>(messages.keySet())) {
                    final Queue<Action0> messages = getQueue(process);
                    while(!messages.isEmpty()) {
                        step(process);
                        result.set(true);
                        progress = true;
                    }
                }

            } while (progress);

            return result.get();
        }

        @Override public String toString() {
            return name;
        }

    }

    @Test public void testInvalidDeadlock1() {
        final Multimap<Process, Set<Process>> detectedDeadlocks = ArrayListMultimap.create();

        final Process p1 = new Process("1", detectedDeadlocks::put);
        final Process p2 = new Process("2", detectedDeadlocks::put);
        final Process p3 = new Process("3", detectedDeadlocks::put);

        p1.setDependencies(p1, p2, p3);
        p2.setDependencies(p2, p3);
        p3.setDependencies(p2, p3);

        p2.cmh.idle();
        p3.cmh.idle();

        boolean progress;
        do {
            progress = p2.processAll() || p3.processAll();
        } while (progress);

        // No messages for p1 whatsoever
        assertFalse(p1.processAll());

        // Both p2 and p3 detected deadlock
        assertEquals(2, detectedDeadlocks.size());
        detectedDeadlocks.clear();

        p1.cmh.idle();
        p1.step(p1); // query(i=1, m=1, j=1, k=1)
        p3.step(p1); // query(i=1, m=1, j=1, k=3)
        p1.step(p1); // reply(i=1, m=1, r=1, k=1)
        p2.step(p1); // query(i=1, m=1, j=1, k=2)
        p2.step(p3); // query(i=1, m=1, j=3, k=2)
        p2.step(p2); // query(i=1, m=1, j=2, k=2)
        p2.step(p2); // reply(i=1, m=1, r=2, k=2)
        p3.step(p3); // query(i=1, m=1, j=3, k=3)
        p3.step(p2); // query(i=1, m=1, j=2, k=3)
        p3.step(p2); // reply(i=1, m=1, r=2, k=3)

        p3.step(p3); // reply(i=1, m=1, r=3, k=3)
        p1.step(p3); // reply(i=1, m=1, r=3, k=1)
        p2.step(p3); // reply(i=1, m=1, r=3, k=2)
        p1.step(p2); // reply(i=1, m=1, r=2, k=1)

        assertTrue(detectedDeadlocks.isEmpty());
    }

    @Test public void testNoInvalidDeadlock2() {
        final Multimap<Process, Set<Process>> detectedDeadlocks = ArrayListMultimap.create();

        final Process p1 = new Process("1", detectedDeadlocks::put);
        final Process p2 = new Process("2", detectedDeadlocks::put);
        final Process p3 = new Process("3", detectedDeadlocks::put);

        p1.setDependencies(p1, p2, p3);
        p2.setDependencies(p2, p3);
        p3.setDependencies(p2, p3);

        p2.cmh.idle();
        p3.cmh.idle();

        boolean progress;
        do {
            progress = p2.processAll() || p3.processAll();
        } while (progress);

        // No messages for p1 whatsoever
        assertFalse(p1.processAll());

        // Both p2 and p3 detected deadlock
        assertEquals(2, detectedDeadlocks.size());
        detectedDeadlocks.clear();

        p1.cmh.idle();
        p1.step(p1);
        p1.step(p1);
        p3.step(p1);
        p2.step(p1);
        p3.step(p3);
        p3.step(p2);
        // p3.cmh.exec();
        p2.step(p3);
        p2.step(p2);
        p2.step(p3);
        p2.step(p2);
        p1.step(p2);

        p3.step(p3);
        p3.step(p2);
        p1.step(p3);


        assertTrue(detectedDeadlocks.isEmpty());
    }

}
