package mb.nabl2.constraints;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;

import mb.nabl2.constraints.ast.AstConstraints;
import mb.nabl2.constraints.base.BaseConstraints;
import mb.nabl2.constraints.base.CConj;
import mb.nabl2.constraints.equality.EqualityConstraints;
import mb.nabl2.constraints.nameresolution.NameResolutionConstraints;
import mb.nabl2.constraints.poly.PolyConstraints;
import mb.nabl2.constraints.relations.RelationConstraints;
import mb.nabl2.constraints.scopegraph.ScopeGraphConstraints;
import mb.nabl2.constraints.sets.SetConstraints;
import mb.nabl2.constraints.sym.SymbolicConstraints;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.substitution.ISubstitution;

public class Constraints {

    // TODO Remove after bootstrapping
    public static IMatcher<IConstraint> matchConstraintOrList() {
        // @formatter:off
        return M.cases(
            M.listElems(Constraints.matcher(), (l, cs) -> CConj.of(cs)),
            Constraints.matcher()
        );
        // @formatter:on
    }

    public static IMatcher<IConstraint> matcher() {
        // @formatter:off
        return M.req("Not a constraint", M.<IConstraint>cases(
            AstConstraints.matcher(),
            BaseConstraints.matcher(),
            EqualityConstraints.matcher(),
            ScopeGraphConstraints.matcher(),
            NameResolutionConstraints.matcher(),
            RelationConstraints.matcher(),
            SetConstraints.matcher(),
            SymbolicConstraints.matcher(),
            PolyConstraints.matcher()
        ));
        // @formatter:on
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
            ScopeGraphConstraints::build,
            NameResolutionConstraints::build,
            RelationConstraints::build,
            SetConstraints::build,
            SymbolicConstraints::build,
            PolyConstraints::build
            // @formatter:on
        ));
    }

    public static ITerm buildAll(Collection<IConstraint> constraints) {
        List<ITerm> constraintTerms = constraints.stream().map(Constraints::build).collect(Collectors.toList());
        return B.newAppl("Constraints", (ITerm) B.newList(constraintTerms));
    }

    public static ITerm buildPriority(int prio) {
        return B.newString(String.join("", Collections.nCopies(prio, "!")));
    }

    public static IConstraint substitute(IConstraint constraint, ISubstitution.Immutable subst) {
        return subst.isEmpty() ? constraint : constraint.match(IConstraint.Cases.<IConstraint>of(
        // @formatter:off
            c -> AstConstraints.substitute(c, subst),
            c -> BaseConstraints.substitute(c, subst),
            c -> EqualityConstraints.substitute(c, subst),
            c -> ScopeGraphConstraints.substitute(c, subst),
            c -> NameResolutionConstraints.substitute(c, subst),
            c -> RelationConstraints.substitute(c, subst),
            c -> SetConstraints.substitute(c, subst),
            c -> SymbolicConstraints.substitute(c, subst),
            c -> PolyConstraints.substitute(c, subst)
            // @formatter:on
        ));
    }

    public static IConstraint transform(IConstraint constraint, Function1<ITerm, ITerm> map) {
        return constraint.match(IConstraint.Cases.<IConstraint>of(
        // @formatter:off
            c -> AstConstraints.transform(c, map),
            c -> BaseConstraints.transform(c, map),
            c -> EqualityConstraints.transform(c, map),
            c -> ScopeGraphConstraints.transform(c, map),
            c -> NameResolutionConstraints.transform(c, map),
            c -> RelationConstraints.transform(c, map),
            c -> SetConstraints.transform(c, map),
            c -> SymbolicConstraints.transform(c, map),
            c -> PolyConstraints.transform(c, map)
            // @formatter:on
        ));
    }

}