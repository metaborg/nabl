package mb.statix.spoofax;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Set;

public class StatixProjectConfig implements IStatixProjectConfig {

    private final Set.Immutable<String> parallelLanguages;
    private final Integer messageStacktraceLength;
    private final Integer messageTermDepth;

    public StatixProjectConfig(Iterable<String> parallelLanguages, Integer messageStackTrace,
            Integer messageTermDepth) {
        this.parallelLanguages = parallelLanguages != null ? CapsuleUtil.toSet(parallelLanguages) : null;
        this.messageStacktraceLength = messageStackTrace;
        this.messageTermDepth = messageTermDepth;
    }

    @Override public java.util.Set<String> parallelLanguages(java.util.Set<String> defaultValue) {
        return parallelLanguages != null ? parallelLanguages : defaultValue;
    }

    @Override public Integer messageTraceLength(Integer defaultValue) {
        return messageStacktraceLength != null ? messageStacktraceLength : defaultValue;
    }

    @Override public Integer messageTermDepth(Integer defaultValue) {
        return messageTermDepth != null ? messageTermDepth : defaultValue;
    }


}