package mb.statix.spoofax;

import java.util.Map;
import java.util.Optional;

import org.metaborg.util.collection.CapsuleUtil;

public class StatixProjectConfig implements IStatixProjectConfig {

    private final io.usethesource.capsule.Map.Immutable<String, SolverMode> languageModes;
    private final Integer messageStacktraceLength;
    private final Integer messageTermDepth;
    private final String testLogLevel;
    private final Boolean suppressCascadingErrors;

    public StatixProjectConfig(Map<String, SolverMode> languageModes, Integer messageStackTrace,
            Integer messageTermDepth, String testLogLevel, Boolean suppressCascadingErrors) {
        this.languageModes = languageModes != null ? CapsuleUtil.toMap(languageModes) : null;
        this.messageStacktraceLength = messageStackTrace;
        this.messageTermDepth = messageTermDepth;
        this.testLogLevel = testLogLevel;
        this.suppressCascadingErrors = suppressCascadingErrors;
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

    @Override public String testLogLevel(String defaultValue) {
        return Optional.ofNullable(testLogLevel).orElse(defaultValue);
    }

    @Override public Boolean suppressCascadingErrors() {
        return suppressCascadingErrors;
    }

}
