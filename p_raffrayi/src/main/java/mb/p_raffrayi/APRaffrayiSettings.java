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

    // Overriding here avoids Immutables generating code dependent on guava... might be removed once guava is fully off the classpath
    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StrategoAnnotations");
        sb.append("(");
        sb.append("recording=" + recording());
        sb.append("incrementalDeadlock=" + incrementalDeadlock());
        sb.append("scopeGraphDiff=" + scopeGraphDiff());
        sb.append("confirmation=" + confirmation());
        sb.append(")");
        return sb.toString();
    }
}
