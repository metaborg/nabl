package org.metaborg.meta.nabl2.constraints;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.meta.nabl2.constraints.ast.AstConstraints;
import org.metaborg.meta.nabl2.constraints.base.BaseConstraints;
import org.metaborg.meta.nabl2.constraints.equality.EqualityConstraints;
import org.metaborg.meta.nabl2.constraints.namebinding.NamebindingConstraints;
import org.metaborg.meta.nabl2.constraints.poly.PolyConstraints;
import org.metaborg.meta.nabl2.constraints.relations.RelationConstraints;
import org.metaborg.meta.nabl2.constraints.sets.SetConstraints;
import org.metaborg.meta.nabl2.constraints.sym.SymbolicConstraints;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.unification.IUnifier;

public class Constraints {

    public static IMatcher<IConstraint> matcher() {
        return M.<IConstraint>cases(
            // @formatter:off
            AstConstraints.matcher(),
            BaseConstraints.matcher(),
            EqualityConstraints.matcher(),
            NamebindingConstraints.matcher(),
            RelationConstraints.matcher(),
            SetConstraints.matcher(),
            SymbolicConstraints.matcher(),
            PolyConstraints.matcher()
            // @formatter:on
        );
    }

    public static IMatcher<Integer> priorityMatcher() {
        return M.string(s -> s.getValue().length());
    }

    public static ITerm build(IConstraint constraint) {
        return constraint.match(IConstraint.Cases.<ITerm>of(
            // @formatter:off
            AstConstraints::build,
            BaseConstraints::build,
            EqualityConstraints::build,
            NamebindingConstraints::build,
            RelationConstraints::build,
            SetConstraints::build,
            SymbolicConstraints::build,
            PolyConstraints::build
            // @formatter:on
        ));
    }

    public static ITerm build(Collection<IConstraint> constraints) {
        List<ITerm> constraintTerms = constraints.stream().map(Constraints::build).collect(Collectors.toList());
        return TB.newAppl("Constraints", (ITerm) TB.newList(constraintTerms));
    }

    public static ITerm buildPriority(int prio) {
        return TB.newString(String.join("", Collections.nCopies(prio, "!")));
    }

    public static IConstraint find(IConstraint constraint, IUnifier unifier) {
        return constraint.match(IConstraint.Cases.<IConstraint>of(
            // @formatter:off
            c -> AstConstraints.find(c, unifier),
            c -> BaseConstraints.find(c, unifier),
            c -> EqualityConstraints.find(c, unifier),
            c -> NamebindingConstraints.find(c, unifier),
            c -> RelationConstraints.find(c, unifier),
            c -> SetConstraints.find(c, unifier),
            c -> SymbolicConstraints.find(c, unifier),
            c -> PolyConstraints.find(c, unifier)
            // @formatter:on
        ));
    }

}