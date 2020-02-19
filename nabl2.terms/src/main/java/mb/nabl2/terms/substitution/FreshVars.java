package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;

/**
 * Class to generate fresh names, possibly relative to an already existing set of names. Generated fresh names are
 * remembered, so subsequent calls do not generate the same fresh names. If the given name is already fresh, the name is
 * kept unchanged.
 */
public class FreshVars {

    private static final String STRIP_RE = "[0-9\\_\\-]*$";

    private final Set.Transient<ITermVar> oldVars;
    private Set.Transient<ITermVar> newVars;

    public FreshVars() {
        this.oldVars = Set.Transient.of();
        this.newVars = Set.Transient.of();
    }

    public FreshVars(Iterable<ITermVar> oldVars) {
        this();
        oldVars.forEach(this.oldVars::__insert);
    }

    /**
     * Generate a variable with a fresh name, and remember the generated name.
     */
    public ITermVar fresh(String name) {
        final String base = name.replaceAll(STRIP_RE, "");
        ITermVar fresh = B.newVar("", name);
        int i = 0;
        while(oldVars.contains(fresh) || newVars.contains(fresh)) {
            fresh = B.newVar("", base + "-" + Integer.toString(i++));
        }
        newVars.__insert(fresh);
        return fresh;
    }

    /**
     * Generate variables with fresh names, ensuring generated names do not overlap with variables in the set of
     * freshened variables. Generated names are remembered. Returns a renaming from variables to fresh variables.
     */
    public IRenaming.Immutable fresh(java.util.Set<ITermVar> vars) {
        final IRenaming.Transient renaming = PersistentRenaming.Transient.of();
        for(ITermVar var : vars) {
            final String base = var.getName().replaceAll(STRIP_RE, "");
            ITermVar fresh = var;
            int i = 0;
            while((vars.contains(fresh) && !var.equals(fresh)) || oldVars.contains(fresh) || newVars.contains(fresh)) {
                fresh = B.newVar(var.getResource(), base + Integer.toString(i++));
            }
            newVars.__insert(fresh);
            renaming.put(var, fresh);
        }
        return renaming.freeze();
    }

    /**
     * Keep until now generated fresh names even when reset() is called later.
     */
    public Set.Immutable<ITermVar> fix() {
        final Set.Immutable<ITermVar> fixedVars = newVars.freeze();
        oldVars.__insertAll(fixedVars);
        this.newVars = Set.Transient.of();
        return fixedVars;
    }

    /**
     * Reset the state to what it was at the last call to fix() or when the object was created.
     */
    public Set.Immutable<ITermVar> reset() {
        final Set.Immutable<ITermVar> resetVars = newVars.freeze();
        this.newVars = Set.Transient.of();
        return resetVars;
    }

}