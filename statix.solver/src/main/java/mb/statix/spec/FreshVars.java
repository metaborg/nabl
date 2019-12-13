package mb.statix.spec;

import static mb.nabl2.terms.build.TermBuild.B;

import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.IRenaming;
import mb.nabl2.terms.substitution.Renaming;

public class FreshVars {

    private final Set.Transient<ITermVar> vars;
    private Renaming.Builder newVars;

    public FreshVars() {
        this.vars = Set.Transient.of();
        this.newVars = Renaming.builder();
    }

    public FreshVars(Iterable<ITermVar> vars) {
        this();
        vars.forEach(this.vars::__insert);
    }

    public ITermVar freshNew(String name) {
        final String base = name.replaceAll("-?[0-9]*$", "");
        ITermVar fresh = B.newVar("", name);
        int i = 0;
        while(vars.contains(fresh) || newVars.containsKey(fresh) || newVars.containsValue(fresh)) {
            fresh = B.newVar("", base + "-" + Integer.toString(i++));
        }
        newVars.put(fresh, fresh);
        return fresh;
    }

    public ITermVar freshExisting(ITermVar var) {
        final String base = var.getName().replaceAll("-?[0-9]*$", "");
        ITermVar fresh = var;
        int i = 0;
        while(vars.contains(fresh) || newVars.containsKey(fresh) || newVars.containsValue(fresh)) {
            fresh = B.newVar(var.getResource(), base + "-" + Integer.toString(i++));
        }
        newVars.put(var, fresh);
        return fresh;
    }

    public java.util.Set<ITermVar> fix() {
        final java.util.Set<ITermVar> fixedVars = newVars.build().keySet();
        vars.__insertAll(fixedVars);
        this.newVars = Renaming.builder();
        return fixedVars;
    }

    public IRenaming reset() {
        final IRenaming renaming = newVars.build();
        this.newVars = Renaming.builder();
        return renaming;
    }

}