package mb.nabl2.terms.substitution;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.metaborg.util.collection.CapsuleUtil;

import com.google.common.collect.Lists;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;

/**
 * Class to generate fresh names, possibly relative to an already existing set of names. Generated fresh names are
 * remembered, so subsequent calls do not generate the same fresh names. If the given name is already fresh, the name is
 * kept unchanged.
 */
public class FreshVars {

    private List<java.util.Set<ITermVar>> oldVarSets;
    private Set.Immutable<ITermVar> oldVars;
    private Set.Immutable<ITermVar> newVars;

    public FreshVars() {
        this.oldVarSets = new ArrayList<>();
        this.oldVars = CapsuleUtil.immutableSet();
        this.newVars = CapsuleUtil.immutableSet();
    }

    @SafeVarargs public FreshVars(java.util.Set<ITermVar>... preExistingVarSets) {
        this.oldVarSets = new ArrayList<>(Arrays.asList(preExistingVarSets));
        this.oldVars = CapsuleUtil.immutableSet();
        this.newVars = CapsuleUtil.immutableSet();
    }

    public FreshVars(Iterable<ITermVar> preExistingVars) {
        this.oldVarSets = new ArrayList<>();
        this.oldVars = CapsuleUtil.toSet(preExistingVars);
        this.newVars = CapsuleUtil.immutableSet();
    }

    /**
     * Add pre-existing variables, which cannot be used as fresh names.
     */
    public void add(Set<ITermVar> preExistingVarSet) {
        oldVarSets.add(preExistingVarSet);
    }

    /**
     * Add pre-existing variables, which cannot be used as fresh names.
     */
    public void add(Iterable<ITermVar> preExistingVars) {
        final Set.Transient<ITermVar> oldVars = this.oldVars.asTransient();
        for(ITermVar var : preExistingVars) {
            oldVars.__insert(var);
        }
        this.oldVars = oldVars.freeze();
    }

    /**
     * Generate a variable with a fresh name, and remember the generated name.
     */
    public ITermVar fresh(String name) {
        final String base = dropSuffix(name);
        ITermVar fresh = B.newVar("", name);
        int i = 0;
        while(notFresh(fresh)) {
            fresh = B.newVar("", base + "-" + (i++));
        }
        newVars = newVars.__insert(fresh);
        return fresh;
    }

    public ITermVar fresh(ITermVar var) {
        final String base = dropSuffix(var.getName());
        ITermVar fresh = var;
        int i = 0;
        while(notFresh(fresh)) {
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
            while((vars.contains(fresh) && !var.equals(fresh)) || notFresh(fresh)) {
                fresh = B.newVar(var.getResource(), base + "-" + (i++));
            }
            newVars = newVars.__insert(fresh);
            renaming.put(var, fresh);
            renaming.put(fresh, var);
        }
        return renaming.build();
    }

    private boolean notFresh(ITermVar var) {
        return oldVars.contains(var) || newVars.contains(var) || oldVarSets.stream().anyMatch(s -> s.contains(var));
    }

    /**
     * Keep until now generated fresh names even when reset() is called later.
     */
    public Set.Immutable<ITermVar> fix() {
        final Set.Immutable<ITermVar> fixedVars = newVars;
        oldVars = oldVars.__insertAll(fixedVars);
        this.newVars = CapsuleUtil.immutableSet();
        return fixedVars;
    }

    /**
     * Reset the state to what it was at the last call to fix() or when the object was created.
     */
    public Set.Immutable<ITermVar> reset() {
        final Set.Immutable<ITermVar> resetVars = newVars;
        this.newVars = CapsuleUtil.immutableSet();
        return resetVars;
    }

    private String dropSuffix(String name) {
        final int idx = name.indexOf('-');
        return idx < 0 ? name : name.substring(0, idx);
    }

}
