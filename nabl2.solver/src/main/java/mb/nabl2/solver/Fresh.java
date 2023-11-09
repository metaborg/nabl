package mb.nabl2.solver;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Map;

public abstract class Fresh {

    public static class Immutable extends Fresh implements Serializable {

        private static final long serialVersionUID = 42L;

        private final Map.Immutable<String, Integer> counters;

        public Immutable(Map.Immutable<String, Integer> counters) {
            this.counters = counters;
        }

        public Transient melt() {
            final ConcurrentMap<String, Integer> transientCounters = new ConcurrentHashMap<>();
            transientCounters.putAll(counters);
            return new Transient(transientCounters);
        }

        public static Fresh.Immutable of() {
            return new Immutable(Map.Immutable.of());
        }

    }

    public static class Transient extends Fresh {

        private final ConcurrentMap<String, Integer> counters;

        public Transient(ConcurrentMap<String, Integer> counters) {
            this.counters = counters;
        }

        public String fresh(String base) {
            // remove any number suffix from the base
            base = base.replaceFirst("-[0-9]+$", "");
            // to prevent accidental name clashes, ensure the base contains no dashes,
            // and then use dashes as our connecting character.
            base = base.replaceAll("-", "_");
            int k = counters.getOrDefault(base, 0) + 1;
            counters.put(base, k);
            return base + "-" + k;
        }

        public void reset() {
            counters.clear();
        }

        public Immutable freeze() {
            final Map.Transient<String, Integer> transientCounters = CapsuleUtil.transientMap();
            transientCounters.__putAll(counters);
            return new Immutable(transientCounters.freeze());
        }

        public static Fresh.Transient of() {
            return new Transient(new ConcurrentHashMap<>());
        }

    }

}