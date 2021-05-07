package mb.p_raffrayi.impl;

public enum UnitState {
    INIT_UNIT, INIT_TC(true), UNKNOWN, ACTIVE(true), RELEASED(true), DONE;

    private final boolean active;

    private UnitState(boolean active) {
        this.active = active;
    }

    private UnitState() {
        this(false);
    }

    public boolean active() {
        return active;
    }
}