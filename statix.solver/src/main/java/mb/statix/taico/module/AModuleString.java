package mb.statix.taico.module;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import mb.nabl2.terms.substitution.ISubstitution;

@Value.Immutable
public abstract class AModuleString {
    @Value.Parameter public abstract List<IModuleStringComponent> components();
    
    public String build() {
        return components().stream().map(c -> c.toString()).collect(Collectors.joining(""));
    }
    
    public String build(ISubstitution.Immutable subst) {
        return components().stream().map(c -> c.apply(subst).toString()).collect(Collectors.joining(""));
    }
    
    /**
     * Applies the given substitution to this module string.
     * 
     * @param subst
     *      the substitution to apply
     * 
     * @return
     *      a new immutable module string with the given substitution applied
     */
    public ModuleString apply(ISubstitution.Immutable subst) {
        return ModuleString.of(
                components().stream().map(c -> c.apply(subst)).collect(Collectors.toList()));
    }
    
    /**
     * Recreates the <pre>$[someString [var] ...]</pre> string.
     */
    @Override
    public String toString() {
        return "$[" +
                 components().stream()
                     .map(c -> c.toString(t -> "[" + t + "]"))
                     .collect(Collectors.joining("")) +
                 "]";
    }
}