package statix.lang.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        // @formatter:off
        super(new Strategy[] {
            solve_ext_constraints_1_0.instance
        });
        // @formatter:on
    }
}