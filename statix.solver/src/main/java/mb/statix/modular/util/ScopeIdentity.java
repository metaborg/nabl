package mb.statix.modular.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import mb.statix.constraints.CUser;
import mb.statix.solver.IConstraint;
import mb.statix.spec.IRule;

public class ScopeIdentity {
    /**
     * Creates a trace for the given constraint for scope identity.
     * 
     * @param constraint
     *      the constraint
     * @param sb
     *      the stringbuilder to add to
     */
    public static void userTrace(@Nullable IConstraint constraint, StringBuilder sb) {
        //We want to know the trace of IRules that were triggered, that is the runtime path.
        while (constraint != null) {
            if (!(constraint instanceof CUser)) {
                constraint = constraint.cause().orElse(null);
                continue;
            }
            
            CUser user = (CUser) constraint;
            if (!user.isSkipModuleBoundaryUser()) break;
            
            //If no rule was applied, we don't have to continue any more
            IRule rule = user.getAppliedRule();
            if (rule == null) break;
            
            //Collect the rule signature
            sb.append('.').append(rule.signature());
            
            constraint = constraint.cause().orElse(null);
        }
    }
}
