package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.util.CapsuleUtil;

/**
 * Class to generate fresh names, possibly relative to an already existing set of names. Generated fresh names are
 * remembered, so subsequent calls do not generate the same fresh names. If the given name is already fresh, the name is
 * kept unchanged.
 */
public class FreshVars {

    private Set.Immutable<ITermVar> oldVars;
    private Set.Immutable<ITermVar> newVars;

    public FreshVars() {
        this.oldVars = Set.Immutable.of();
        this.newVars = Set.Immutable.of();
    }

    public FreshVars(Iterable<ITermVar> preExistingVars) {
        this.oldVars = CapsuleUtil.toSet(preExistingVars);
        this.newVars = Set.Immutable.of();
    }

    /**
     * Add pre-existing variables, which cannot be used as fresh names.
     */
    public void add(Iterable<ITermVar> preExistingVars) {
        final Set.Transient<ITermVar> newVars = this.newVars.asTransient();
        for(ITermVar var : preExistingVars) {
            if(!oldVars.contains(var)) {
                newVars.__insert(var);
            }
        }
        this.newVars = newVars.freeze();
    }

    /**
     * Generate a variable with a fresh name, and remember the generated name.
     */
    public ITermVar fresh(String name) {
        final String base = dropSuffix(name);
        ITermVar fresh = B.newVar("", name);
        int i = 0;
        while(oldVars.contains(fresh) || newVars.contains(fresh)) {
            fresh = B.newVar("", base + "-" + (i++));
        }
        newVars = newVars.__insert(fresh);
        return fresh;
    }

    public ITermVar fresh(ITermVar var) {
        final String base = dropSuffix(var.getName());
        ITermVar fresh = var;
        int i = 0;
        while(oldVars.contains(fresh) || newVars.contains(fresh)) {
            fresh = B.newVar("", base + "-" + (i++));
        }
        newVars = newVars.__insert(fresh);
        return fresh;
    }

    /**
     * Generate variables with fresh names, ensuring generated names do not overlap with variables in the set of
     * freshened variables. Generated names are remembered. Returns a variable swapping.
     */
    public IRenaming fresh(java.util.Set<ITermVar> vars) {
        final Renaming.Builder renaming = Renaming.builder();
        for(ITermVar var : vars) {
            final String base = var.getName().replaceAll("-?[0-9]*$", "");
            ITermVar fresh = var;
            int i = 0;
            while((vars.contains(fresh) && !var.equals(fresh)) || oldVars.contains(fresh) || newVars.contains(fresh)) {
                fresh = B.newVar(var.getResource(), base + "-" + (i++));
            }
            newVars = newVars.__insert(fresh);
            renaming.put(var, fresh);
            renaming.put(fresh, var);
        }
        return renaming.build();
    }

    /**
     * Keep until now generated fresh names even when reset() is called later.
     */
    public Set.Immutable<ITermVar> fix() {
        final Set.Immutable<ITermVar> fixedVars = newVars;
        oldVars = oldVars.__insertAll(fixedVars);
        this.newVars = Set.Immutable.of();
        return fixedVars;
    }

    /**
     * Reset the state to what it was at the last call to fix() or when the object was created.
     */
    public Set.Immutable<ITermVar> reset() {
        final Set.Immutable<ITermVar> resetVars = newVars;
        this.newVars = Set.Immutable.of();
        return resetVars;
    }

    private String dropSuffix(String name) {
        final int idx = name.indexOf('-');
        return idx < 0 ? name : name.substring(0, idx);
    }

}
