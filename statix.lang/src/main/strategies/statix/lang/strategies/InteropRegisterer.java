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

            // query precompilation
            labelre_to_states_0_2.instance,
            ords_to_relation_0_1.instance,
            labelord_lt_0_1.instance
        });
        // @formatter:on
    }
}
