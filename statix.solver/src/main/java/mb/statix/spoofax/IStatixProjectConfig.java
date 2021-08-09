package mb.statix.spoofax;

import java.util.Map;

public interface IStatixProjectConfig {

    public static int DEFAULT_MESSAGE_TRACE_LENGTH = 0;
    public static int DEFAULT_MESSAGE_TERM_DEPTH = 3;

    public static IStatixProjectConfig NULL = new StatixProjectConfig(null, null, null, null);

    SolverMode languageMode(String languageId, SolverMode defaultMode);

    Map<String, SolverMode> languageModes(Map<String, SolverMode> defaultModes);

    Integer messageTraceLength(Integer defaultValue);

    Integer messageTermDepth(Integer defaultValue);

    String testLogLevel(String defaultValue);

}