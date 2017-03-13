package org.metaborg.meta.nabl2.solver;

import java.io.Serializable;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

public class Fresh implements Serializable {

    private static final long serialVersionUID = 42L;

    private final Object2IntMap<String> counters;

    public Fresh() {
        counters = new Object2IntLinkedOpenHashMap<>();
    }

    public String fresh(String base) {
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

}