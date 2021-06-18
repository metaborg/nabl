package mb.statix.spoofax;

public enum SolverMode {
    // @formatter:off
    TRADITIONAL(false, false, false),
    CONCURRENT(true, false, false),
    INCREMENTAL_DEADLOCK(true, true, false),
    INCREMENTAL_SCOPEGRAPH_DIFF(true, true, true);
    // @formatter:on

    public static SolverMode DEFAULT = TRADITIONAL;

    public final boolean concurrent;
    public final boolean deadlock;
    public final boolean sgDiff;

    private SolverMode(boolean concurrent, boolean deadlock, boolean sgDiff) {
        this.concurrent = concurrent;
        this.deadlock = deadlock;
        this.sgDiff = sgDiff;
    }
}
