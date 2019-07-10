package mb.statix.taico.module;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.util.TermFormatter;

public class ModuleStringVar implements IModuleStringComponent {
    private static final long serialVersionUID = 1L;
    
    private ITerm term;

    public ModuleStringVar(ITerm term) {
        this.term = term;
    }
    
    @Override
    public IModuleStringComponent apply(Immutable subst) {
        ITerm newTerm = subst.apply(term);
        if (term == newTerm) return this;
        return new ModuleStringVar(newTerm);
    }
    
    @Override
    public IModuleStringComponent apply(Transient subst) {
        ITerm newTerm = subst.apply(term);
        if (term == newTerm) return this;
        return new ModuleStringVar(newTerm);
    }
    
    @Override
    public String toString(TermFormatter formatter) {
        return formatter.format(term).replace("\"", "");
    }
    
    @Override
    public String toString() {
        return toString(ITerm::toString);
    }
}
