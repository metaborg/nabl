package mb.statix.taico.module;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.io.Serializable;

import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.spoofax.StatixTerms;

public interface IModuleStringComponent extends Serializable {
    IModuleStringComponent apply(ISubstitution.Immutable subst);
    IModuleStringComponent apply(ISubstitution.Transient subst);
    
    String toString(TermFormatter formatter);
    
    static IMatcher<IModuleStringComponent> matcher() {
        return M.cases(
            M.appl1("Str", M.stringValue(), (t, s) -> new ModuleStringString(s)),
            StatixTerms.varTerm().map(v -> new ModuleStringVar(v))
        );
    }
}
