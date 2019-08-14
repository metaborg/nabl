package statix.lang.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        // @formatter:off
        super(new Strategy[] {
            group_by_2_0.instance
        });
        // @formatter:on
    }
}