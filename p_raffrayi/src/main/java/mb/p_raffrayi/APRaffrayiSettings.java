package mb.p_raffrayi;

import java.util.stream.Collectors;

import org.immutables.value.Value;

@Value.Immutable
public abstract class APRaffrayiSettings {

    @Value.Parameter public abstract boolean recording();

    @Value.Parameter public abstract boolean incrementalDeadlock();

    @Value.Parameter public abstract boolean scopeGraphDiff();

    @Value.Parameter public abstract boolean confirmation();

    public boolean isIncremental() {
        return incrementalDeadlock() || scopeGraphDiff();
    }

    public static PRaffrayiSettings concurrent() {
        return PRaffrayiSettings.of(false, false, false, false);
    }

    public static PRaffrayiSettings concurrentWithRecording() {
        return concurrent().withRecording(true);
    }

    public static PRaffrayiSettings incremental() {
        return PRaffrayiSettings.of(true, true, true, true);
    }
}
