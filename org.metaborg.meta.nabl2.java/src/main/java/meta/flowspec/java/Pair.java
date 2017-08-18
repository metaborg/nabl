package meta.flowspec.java;

import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Immutable
public abstract class Pair<L,R> {
    @Parameter public abstract L left();
    @Parameter public abstract R right();
}
