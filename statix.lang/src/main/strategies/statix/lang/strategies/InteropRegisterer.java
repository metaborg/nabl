package statix.lang.strategies;

import org.strategoxt.lang.JavaInteropRegisterer;
import org.strategoxt.lang.Strategy;

public class InteropRegisterer extends JavaInteropRegisterer {
    public InteropRegisterer() {
        // @formatter:off
        super(new Strategy[] {
            // permission analysis
            group_by_2_0.instance,
            set_fixed_point_0_1.instance,
            // scope graph schemas
            compute_schema_1_0.instance
        });
        // @formatter:on
    }
}
