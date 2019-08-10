package mb.statix.modular.spec;

import java.util.List;

import org.immutables.serial.Serial;
import org.immutables.value.Value;

import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.util.TermFormatter;
import mb.statix.solver.IConstraint;
import mb.statix.spec.ARule;
import mb.statix.taico.module.ModuleString;
import mb.statix.taico.spec.ModuleBoundary;

/**
 * Class which describes a statix rule.
 * 
 * <pre>modbound ruleName(paramVars) | $[moduleName]:- {bodyVars} constraints.</pre>
 */
@Value.Immutable
@Serial.Version(42L)
public abstract class AModuleBoundary extends ARule {

    @Override
    @Value.Parameter public abstract String name();

    @Override
    @Value.Parameter public abstract List<Pattern> params();

    @Value.Parameter public abstract ModuleString moduleString();

    @Override
    @Value.Parameter public abstract IConstraint body();

    @Override
    public ModuleBoundary apply(ISubstitution.Immutable subst) {
        final ModuleString newModuleString = moduleString().apply(subst);
        final IConstraint newBody = body().apply(subst.removeAll(paramVars()));
        return ModuleBoundary.of(name(), params(), newModuleString, newBody);
    }

    /**
     * Formats this rule where constraints are formatted with the given TermFormatter.
     * 
     * <pre>modbound name(params) | $[moduleName] :- {bodyVars} constraints.</pre>
     * 
     * @param termToString
     *      the term formatter to format constraints with
     * 
     * @return
     *      the string
     */
    @Override public String toString(TermFormatter termToString) {
        final StringBuilder sb = new StringBuilder();
        sb.append("modbound ").append(name()).append("(").append(params()).append(")");
        sb.append(" | ").append(moduleString());
        sb.append(" :- ");
        sb.append(body().toString(termToString));
        sb.append(".");
        return sb.toString();
    }

}