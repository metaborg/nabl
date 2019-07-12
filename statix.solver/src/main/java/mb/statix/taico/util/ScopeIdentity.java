package mb.statix.taico.util;

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
        boolean done = false;
        while(constraint != null) {
            if (!(constraint instanceof CUser)) {
                constraint = constraint.cause().orElse(null);
                continue;
            }
            
            if (done) {
                System.err.println("Encountered user constraint after being done! Inconsistency in the logic.");
            }
            
            CUser user = (CUser) constraint;
            IRule rule = user.getAppliedRule();
            if (rule == null) {
                //We shouldn't have to continue here any more
                done = true;
                continue;
            }
            sb.append('.').append(rule.signature());
            
            constraint = constraint.cause().orElse(null);
        }
    }
}
