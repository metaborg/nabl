package mb.statix.spoofax;

public enum SolverMode {
    TRADITIONAL, CONCURRENT, INCREMENTAL_DEADLOCK;

    public static SolverMode DEFAULT = TRADITIONAL;
}
