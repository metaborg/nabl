package mb.statix.taico.module;

import mb.nabl2.terms.substitution.ISubstitution.Immutable;
import mb.nabl2.terms.substitution.ISubstitution.Transient;
import mb.nabl2.util.TermFormatter;

public class ModuleStringString implements IModuleStringComponent {
    
    private String string;
    
    public ModuleStringString(String string) {
        this.string = string;
    }
    
    @Override
    public IModuleStringComponent apply(Immutable subst) {
        return this;
    }
    
    @Override
    public IModuleStringComponent apply(Transient subst) {
        return this;
    }

    @Override
    public String toString(TermFormatter formatter) {
        return string;
    }
    
    @Override
    public String toString() {
        return string;
    }
}
