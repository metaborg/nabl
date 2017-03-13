package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.ast.ImmutableCAstProperty;
import org.metaborg.meta.nabl2.constraints.base.IBaseConstraint;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
import org.metaborg.meta.nabl2.constraints.equality.IEqualityConstraint;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCInequal;
import org.metaborg.meta.nabl2.constraints.messages.MessageInfo;
import org.metaborg.meta.nabl2.constraints.namebinding.INamebindingConstraint;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGImport;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCResolve;
import org.metaborg.meta.nabl2.constraints.poly.IPolyConstraint;
import org.metaborg.meta.nabl2.constraints.poly.ImmutableCGeneralize;
import org.metaborg.meta.nabl2.constraints.poly.ImmutableCInstantiate;
import org.metaborg.meta.nabl2.constraints.relations.IRelationConstraint;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCEvalFunction;
import org.metaborg.meta.nabl2.constraints.sets.ISetConstraint;
import org.metaborg.meta.nabl2.constraints.sets.ImmutableCDistinct;
import org.metaborg.meta.nabl2.constraints.sets.ImmutableCSubsetEq;
import org.metaborg.meta.nabl2.constraints.sym.ISymbolicConstraint;
import org.metaborg.meta.nabl2.constraints.sym.ImmutableCFact;
import org.metaborg.meta.nabl2.constraints.sym.ImmutableCGoal;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.sets.SetTerms;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.unification.IUnifier;

public class Constraints {

    // match
    
    public static IMatcher<IConstraint> matcher() {
        return M.<IConstraint>cases(
            // @formatter:off
            M.appl1("CTrue", MessageInfo.matcherOnlyOriginTerm(), (c, origin) -> {
                return ImmutableCTrue.of(origin);
            }),
            M.appl1("CFalse", MessageInfo.matcher(), (c, origin) -> {
                return ImmutableCFalse.of(origin);
            }),
            M.appl3("CEqual", M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return ImmutableCEqual.of(term1, term2, origin);
            }),
            M.appl3("CInequal", M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                return ImmutableCInequal.of(term1, term2, origin);
            }),
            M.appl3("CGDecl", M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, decl, scope, origin) -> {
                return ImmutableCGDecl.of(scope, decl, origin);
            }),
            M.appl4("CGDirectEdge", M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, scope1, label, scope2, origin) -> {
                return ImmutableCGDirectEdge.of(scope1, label, scope2, origin);
            }),
            M.appl4("CGAssoc", M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, decl, label, scope, origin) -> {
                return ImmutableCGAssoc.of(decl, label, scope, origin);
            }),
            M.appl4("CGNamedEdge", M.term(), Label.matcher(), M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, ref, label, scope, origin) -> {
                return ImmutableCGImport.of(scope, label, ref, origin);
            }),
            M.appl3("CGRef", M.term(), M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, ref, scope, origin) -> {
                return ImmutableCGRef.of(ref, scope, origin);
            }),
            M.appl3("CResolve", M.term(), M.term(), MessageInfo.matcher(), (c, ref, decl, origin) -> {
                return ImmutableCResolve.of(ref, decl, origin);
            }),
            M.appl4("CAssoc", M.term(), Label.matcher(), M.term(), MessageInfo.matcher(), (c, decl, label, scope, origin) -> {
                return ImmutableCAssoc.of(decl, label, scope, origin);
            }),
            M.appl5("CDeclProperty", M.term(), M.term(), M.term(), priorityMatcher(), MessageInfo.matcher(), (c, decl, key, value, prio, origin) -> {
                return ImmutableCDeclProperty.of(decl, key, value, prio, origin);
            }),
            M.appl4("CBuildRel", M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(), (c, term1, rel, term2, origin) -> {
                return ImmutableCBuildRelation.of(term1, rel, term2, origin);
            }),
            M.appl4("CCheckRel", M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(), (c, term1, rel, term2, origin) -> {
                return ImmutableCCheckRelation.of(term1, rel, term2, origin);
            }),
            M.appl4("CEval", M.term(), RelationTerms.functionName(), M.term(), MessageInfo.matcher(), (c, result, fun, term, origin) -> {
                return ImmutableCEvalFunction.of(result, fun, term, origin);
            }),
            M.appl3("CAstProperty", TermIndex.matcher(), M.term(), M.term(), (c, index, key, value) -> {
                return ImmutableCAstProperty.of(index, key, value, MessageInfo.of(index));
            }),
            M.appl4("CSubsetEq", M.term(), SetTerms.projection(), M.term(), MessageInfo.matcher(), (c, left, proj, right, origin) -> {
                return ImmutableCSubsetEq.of(left, right, proj, origin);
            }),
            M.appl3("CDistinct", SetTerms.projection(), M.term(), MessageInfo.matcher(), (c, proj, set, origin) -> {
                return ImmutableCDistinct.of(set, proj, origin);
            }),
            M.appl2("CFact", M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, fact, origin) -> {
                return ImmutableCFact.of(fact, origin);
            }),
            M.appl2("CGoal", M.term(), MessageInfo.matcherOnlyOriginTerm(), (c, goal, origin) -> {
                return ImmutableCGoal.of(goal, origin);
            }),
            M.appl3("CGen", M.term(), M.term(), MessageInfo.matcher(), (c, scheme, type, origin) -> {
                return ImmutableCGeneralize.of(scheme, type, origin);
            }),
            M.appl3("CInst", M.term(), M.term(), MessageInfo.matcher(), (c, type, scheme, origin) -> {
                return ImmutableCInstantiate.of(type, scheme, origin);
            })
            // @formatter:on
        );
    }

    private static IMatcher<Integer> priorityMatcher() {
        return M.string(s -> s.getValue().length());
    }

    // find
    
    public static IConstraint find(IConstraint constraint, IUnifier unifier) {
        return constraint.match(IConstraint.Cases.<IConstraint>of(
            // @formatter:off
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier),
            c -> find(c, unifier)
            // @formatter:on
        ));
    }

    public static IAstConstraint find(IAstConstraint constraint, IUnifier unifier) {
        return constraint.match(IAstConstraint.Cases.<IAstConstraint>of(
            // @formatter:off
            prop -> ImmutableCAstProperty.of(
                        prop.getIndex(),
                        prop.getKey(),
                        unifier.find(prop.getValue()),
                        prop.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));

    }

    public static IBaseConstraint find(IBaseConstraint constraint, IUnifier unifier) {
        return constraint.match(IBaseConstraint.Cases.<IBaseConstraint>of(
            // @formatter:off
            t -> ImmutableCTrue.of(t.getMessageInfo().apply(unifier::find)),
            f -> ImmutableCTrue.of(f.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static IEqualityConstraint find(IEqualityConstraint constraint, IUnifier unifier) {
        return constraint.match(IEqualityConstraint.Cases.<IEqualityConstraint>of(
            // @formatter:off
            eq -> ImmutableCEqual.of(
                    unifier.find(eq.getLeft()),
                    unifier.find(eq.getRight()),
                    eq.getMessageInfo().apply(unifier::find)),
            ineq -> ImmutableCInequal.of(
                    unifier.find(ineq.getLeft()),
                    unifier.find(ineq.getRight()),
                    ineq.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static INamebindingConstraint find(INamebindingConstraint constraint, IUnifier unifier) {
        return constraint.match(INamebindingConstraint.Cases.<INamebindingConstraint>of(
            // @formatter:off
            decl -> ImmutableCGDecl.of(
                        unifier.find(decl.getScope()),
                        unifier.find(decl.getDeclaration()),
                        decl.getMessageInfo().apply(unifier::find)),
            ref -> ImmutableCGRef.of(
                        unifier.find(ref.getReference()),
                        unifier.find(ref.getScope()),
                        ref.getMessageInfo().apply(unifier::find)),
            edge -> ImmutableCGDirectEdge.of(
                        unifier.find(edge.getSourceScope()),
                        edge.getLabel(),
                        unifier.find(edge.getTargetScope()),
                        edge.getMessageInfo().apply(unifier::find)),
            exp -> ImmutableCGAssoc.of(
                        unifier.find(exp.getDeclaration()),
                        exp.getLabel(),
                        unifier.find(exp.getScope()),
                        exp.getMessageInfo().apply(unifier::find)),
            imp -> ImmutableCGImport.of(
                        unifier.find(imp.getScope()),
                        imp.getLabel(),
                        unifier.find(imp.getReference()),
                        imp.getMessageInfo().apply(unifier::find)),
            res -> ImmutableCResolve.of(
                        unifier.find(res.getReference()),
                        unifier.find(res.getDeclaration()),
                        res.getMessageInfo().apply(unifier::find)),
            assoc -> ImmutableCAssoc.of(
                        unifier.find(assoc.getDeclaration()),
                        assoc.getLabel(),
                        unifier.find(assoc.getScope()),
                        assoc.getMessageInfo().apply(unifier::find)),
            prop -> ImmutableCDeclProperty.of(
                        unifier.find(prop.getDeclaration()),
                        prop.getKey(),
                        unifier.find(prop.getValue()),
                        prop.getPriority(),
                        prop.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static IPolyConstraint find(IPolyConstraint constraint, IUnifier unifier) {
        return constraint.match(IPolyConstraint.Cases.of(
            // @formatter:off
            gen -> ImmutableCGeneralize.of(
                        unifier.find(gen.getScheme()),
                        unifier.find(gen.getType()),
                        gen.getMessageInfo().apply(unifier::find)),
            inst -> ImmutableCInstantiate.of(
                        unifier.find(inst.getType()),
                        unifier.find(inst.getScheme()),
                        inst.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static IRelationConstraint find(IRelationConstraint constraint, IUnifier unifier) {
        return constraint.match(IRelationConstraint.Cases.<IRelationConstraint>of(
            // @formatter:off
            build -> ImmutableCBuildRelation.of(
                        unifier.find(build.getLeft()),
                        build.getRelation(),
                        unifier.find(build.getRight()),
                        build.getMessageInfo().apply(unifier::find)),
            check -> ImmutableCCheckRelation.of(
                        unifier.find(check.getLeft()),
                        check.getRelation(),
                        unifier.find(check.getRight()),
                        check.getMessageInfo().apply(unifier::find)),
            eval -> ImmutableCEvalFunction.of(
                        unifier.find(eval.getResult()),
                        eval.getFunction(),
                        unifier.find(eval.getTerm()),
                        eval.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static ISetConstraint find(ISetConstraint constraint, IUnifier unifier) {
        return constraint.match(ISetConstraint.Cases.of(
            // @formatter:off
            subseteq -> ImmutableCSubsetEq.of(
                            unifier.find(subseteq.getLeft()),
                            unifier.find(subseteq.getRight()),
                            subseteq.getProjection(),
                            subseteq.getMessageInfo().apply(unifier::find)),
            distinct -> ImmutableCDistinct.of(
                            unifier.find(distinct.getSet()),
                            distinct.getProjection(),
                            distinct.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

    public static ISymbolicConstraint find(ISymbolicConstraint constraint, IUnifier unifier) {
        return constraint.match(ISymbolicConstraint.Cases.<ISymbolicConstraint>of(
            // @formatter:off
            fact -> ImmutableCFact.of(
                        unifier.find(fact.getFact()),
                        fact.getMessageInfo().apply(unifier::find)),
            goal ->  ImmutableCGoal.of(
                        unifier.find(goal.getGoal()),
                        goal.getMessageInfo().apply(unifier::find))
            // @formatter:on
        ));
    }

}