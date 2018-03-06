package org.metaborg.meta.nabl2.config;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class NaBL2DebugConfig implements Serializable {
    private static final long serialVersionUID = 42L;

    public static final NaBL2DebugConfig NONE = new NaBL2DebugConfig(EnumSet.noneOf(Flag.class));

    public enum Flag {

        ALL, ANALYSIS, FILES, COLLECTION, RESOLUTION, CUSTOM, TIMING;

        public static EnumSet<Flag> valuesOf(Collection<String> names) {
            Set<Flag> flags = names.stream().map(String::toUpperCase).map(Flag::valueOf).collect(Collectors.toSet());
            return Sets.newEnumSet(flags, Flag.class);
        }

        public static List<String> namesOf(Collection<Flag> flags) {
            return flags.stream().map(Flag::name).map(String::toLowerCase).collect(Collectors.toList());
        }

    }

    private final EnumSet<Flag> flags;

    public NaBL2DebugConfig(Flag... flags) {
        this(Arrays.asList(flags));
    }

    public NaBL2DebugConfig(Collection<Flag> flags) {
        this.flags = Sets.newEnumSet(flags, Flag.class);
    }

    public boolean collection() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.COLLECTION);
    }

    public boolean resolution() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.RESOLUTION);
    }

    public boolean timing() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.TIMING);
    }

    public boolean files() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.FILES);
    }

    public boolean analysis() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.ANALYSIS);
    }

    public boolean custom() {
        return flags.contains(Flag.ALL) || flags.contains(Flag.CUSTOM);
    }

    public static NaBL2DebugConfig of(Collection<Flag> flags) {
        return new NaBL2DebugConfig(flags);
    }

    public Collection<Flag> flags() {
        return flags;
    }

}
