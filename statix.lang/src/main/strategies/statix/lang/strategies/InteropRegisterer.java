package statix.lang.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        // @formatter:off
        super(new Strategy[] {
            group_by_2_0.instance,
            set_fixed_point_0_1.instance,
            solve_typec_1_2.instance
        });
        // @formatter:on
    }
}
