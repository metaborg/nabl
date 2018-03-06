package org.metaborg.meta.nabl2.solver.properties;

import org.metaborg.meta.nabl2.constraints.namebinding.DeclProperties;
import org.metaborg.meta.nabl2.scopegraph.terms.Occurrence;
import org.metaborg.meta.nabl2.solver.components.NameResolutionComponent;
import org.metaborg.meta.nabl2.terms.ITerm;

public class PolySafe {

    private final ActiveVars activeVars;
    private final ActiveDeclTypes activeDeclTypes;
    private final NameResolutionComponent nameResolutionSolver;

    public PolySafe(ActiveVars activeVars, ActiveDeclTypes activeDeclTypes,
            NameResolutionComponent nameResolutionSolver) {
        this.activeVars = activeVars;
        this.activeDeclTypes = activeDeclTypes;
        this.nameResolutionSolver = nameResolutionSolver;
    }

    public boolean isGenSafe(ITerm t) {
        return activeVars.isNotActive(t)
                && nameResolutionSolver.getDeps(t).stream().allMatch(decl -> activeDeclTypes.isNotActive(decl));
    }

    public boolean isInstSafe(Occurrence d) {
        return activeDeclTypes.isNotActive(d) && nameResolutionSolver.getProperty(d, DeclProperties.TYPE_KEY)
                .map(ty -> ty.getVars().stream().allMatch(v -> activeVars.isNotActive(v))).orElse(true);
    }

}
