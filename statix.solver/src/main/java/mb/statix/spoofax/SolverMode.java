package mb.statix.spoofax;

public enum SolverMode {
    // @formatter:off
    TRADITIONAL(false, false, false, false),
    CONCURRENT(true, false, false, false),
    INCREMENTAL_DEADLOCK(true, true, false, false),
    INCREMENTAL_SCOPEGRAPH_DIFF(true, true, true, false),
    INCREMENTAL_SIMPLE_CONFIRMATION(true, true, true, true);
    // @formatter:on

    public static final SolverMode DEFAULT = TRADITIONAL;
    public static final SolverMode FULL = INCREMENTAL_SIMPLE_CONFIRMATION;

    public final boolean concurrent;
    public final boolean deadlock;
    public final boolean sgDiff;
    public final boolean confirmation;

    private SolverMode(boolean concurrent, boolean deadlock, boolean sgDiff, boolean confirmation) {
        this.concurrent = concurrent;
        this.deadlock = deadlock;
        this.sgDiff = sgDiff;
        this.confirmation = confirmation;
    }
}
