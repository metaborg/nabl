package mb.statix.spoofax;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;

public class StatixProjectConfig implements IStatixProjectConfig {

    private final io.usethesource.capsule.Map.Immutable<String, SolverMode> languageModes;
    private final Integer messageStacktraceLength;
    private final Integer messageTermDepth;

    public StatixProjectConfig(Map<String, SolverMode> languageModes, Integer messageStackTrace,
            Integer messageTermDepth) {
        this.languageModes = languageModes != null ? CapsuleUtil.toMap(languageModes) : null;
        this.messageStacktraceLength = messageStackTrace;
        this.messageTermDepth = messageTermDepth;
    }

    @Override public Integer messageTraceLength(Integer defaultValue) {
        return messageStacktraceLength != null ? messageStacktraceLength : defaultValue;
    }

    @Override public Integer messageTermDepth(Integer defaultValue) {
        return messageTermDepth != null ? messageTermDepth : defaultValue;
    }

    @Override public SolverMode languageMode(String languageId, SolverMode defaultMode) {
        return Optional.ofNullable(languageModes.get(languageId)).orElse(defaultMode);
    }

    @Override public Map<String, SolverMode> languageModes(Map<String, SolverMode> defaultModes) {
        return languageModes != null ? languageModes : defaultModes;
    }

}