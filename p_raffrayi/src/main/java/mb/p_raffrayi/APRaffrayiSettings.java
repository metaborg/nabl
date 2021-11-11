package mb.p_raffrayi;

import org.immutables.value.Value;

@Value.Immutable
public abstract class APRaffrayiSettings {

    @Value.Parameter public abstract boolean recording();

    @Value.Parameter public abstract boolean incrementalDeadlock();

    @Value.Parameter public abstract boolean scopeGraphDiff();

    @Value.Parameter public abstract ConfirmationMode confirmationMode();

    public boolean incremental() {
        return incrementalDeadlock() || scopeGraphDiff();
    }

    public static PRaffrayiSettings concurrent() {
        return PRaffrayiSettings.of(false, false, false, ConfirmationMode.TRIVIAL);
    }

    public static PRaffrayiSettings concurrentWithRecording() {
        return PRaffrayiSettings.of(true, false, false, ConfirmationMode.TRIVIAL);
    }

    public enum ConfirmationMode {
        TRIVIAL, SIMPLE_ENVIRONMENT
    }

}
