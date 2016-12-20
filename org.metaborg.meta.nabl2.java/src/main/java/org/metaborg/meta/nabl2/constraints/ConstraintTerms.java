package org.metaborg.meta.nabl2.constraints;

import org.metaborg.meta.nabl2.constraints.ast.ImmutableCAstProperty;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCFalse;
import org.metaborg.meta.nabl2.constraints.base.ImmutableCTrue;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCEqual;
import org.metaborg.meta.nabl2.constraints.equality.ImmutableCInequal;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCDeclProperty;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGAssoc;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDecl;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGDirectEdge;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGImport;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCGRef;
import org.metaborg.meta.nabl2.constraints.namebinding.ImmutableCResolve;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCBuildRelation;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCCheckRelation;
import org.metaborg.meta.nabl2.constraints.relations.ImmutableCEvalFunction;
import org.metaborg.meta.nabl2.constraints.sets.ImmutableCDistinct;
import org.metaborg.meta.nabl2.constraints.sets.ImmutableCSubsetEq;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.relations.terms.RelationTerms;
import org.metaborg.meta.nabl2.scopegraph.terms.Label;
import org.metaborg.meta.nabl2.sets.SetTerms;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class ConstraintTerms {

    public static IMatcher<Iterable<IConstraint>> constraints() {
        return M.listElems(constraint(), (l, cs) -> cs);
    }

    public static IMatcher<IConstraint> constraint() {
        return M.cases(
                // @formatter:off
                M.appl1("CTrue", MessageInfo.simpleMatcher(), (c, origin) -> {
                    return ImmutableCTrue.of(origin);
                }), M.appl1("CFalse", MessageInfo.matcher(), (c, origin) -> {
                    return ImmutableCFalse.of(origin);
                }), M.appl3("CEqual", M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                    return ImmutableCEqual.of(term1, term2, origin);
                }), M.appl3("CInequal", M.term(), M.term(), MessageInfo.matcher(), (c, term1, term2, origin) -> {
                    return ImmutableCInequal.of(term1, term2, origin);
                }), M.appl3("CGDecl", M.term(), M.term(), MessageInfo.simpleMatcher(), (c, decl, scope, origin) -> {
                    return ImmutableCGDecl.of(scope, decl, origin);
                }), M.appl4("CGDirectEdge", M.term(), Label.matcher(), M.term(), MessageInfo.simpleMatcher(),
                        (c, scope1, label, scope2, origin) -> {
                            return ImmutableCGDirectEdge.of(scope1, label, scope2, origin);
                        }),
                M.appl4("CGAssoc", M.term(), Label.matcher(), M.term(), MessageInfo.simpleMatcher(),
                        (c, decl, label, scope, origin) -> {
                            return ImmutableCGAssoc.of(decl, label, scope, origin);
                        }),
                M.appl4("CGNamedEdge", M.term(), Label.matcher(), M.term(), MessageInfo.simpleMatcher(),
                        (c, ref, label, scope, origin) -> {
                            return ImmutableCGImport.of(scope, label, ref, origin);
                        }),
                M.appl3("CGRef", M.term(), M.term(), MessageInfo.simpleMatcher(), (c, ref, scope, origin) -> {
                    return ImmutableCGRef.of(ref, scope, origin);
                }), M.appl3("CResolve", M.term(), M.term(), MessageInfo.matcher(), (c, ref, decl, origin) -> {
                    return ImmutableCResolve.of(ref, decl, origin);
                }), M.appl4("CAssoc", M.term(), Label.matcher(), M.term(), MessageInfo.matcher(),
                        (c, decl, label, scope, origin) -> {
                            return ImmutableCAssoc.of(decl, label, scope, origin);
                        }),
                M.appl5("CDeclProperty", M.term(), M.term(), M.term(), priority(), MessageInfo.matcher(),
                        (c, decl, key, value, prio, origin) -> {
                            return ImmutableCDeclProperty.of(decl, key, value, prio, origin);
                        }),
                M.appl4("CBuildRel", M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(),
                        (c, term1, rel, term2, origin) -> {
                            return ImmutableCBuildRelation.of(term1, rel, term2, origin);
                        }),
                M.appl4("CCheckRel", M.term(), RelationName.matcher(), M.term(), MessageInfo.matcher(),
                        (c, term1, rel, term2, origin) -> {
                            return ImmutableCCheckRelation.of(term1, rel, term2, origin);
                        }),
                M.appl4("CEval", M.term(), RelationTerms.functionName(), M.term(), MessageInfo.matcher(),
                        (c, result, fun, term, origin) -> {
                            return ImmutableCEvalFunction.of(result, fun, term, origin);
                        }),
                M.appl3("CAstProperty", TermIndex.matcher(), M.term(), M.term(), (c, index, key, value) -> {
                    return ImmutableCAstProperty.of(index, key, value, ImmutableMessageInfo.of(index));
                }), M.appl4("CSubsetEq", M.term(), SetTerms.projection(), M.term(), MessageInfo.matcher(),
                        (c, left, proj, right, origin) -> {
                            return ImmutableCSubsetEq.of(left, right, proj, origin);
                        }),
                M.appl3("CDistinct", SetTerms.projection(), M.term(), MessageInfo.matcher(), (c, proj, set, origin) -> {
                    return ImmutableCDistinct.of(set, proj, origin);
                }), M.term(t -> {
                    ILogger logger = LoggerUtils.logger(ConstraintTerms.class);
                    logger.warn("Ignoring constraint: {}", t);
                    return ImmutableCTrue.of(ImmutableMessageInfo.of(t));
                })
        // @formatter:on
        );
    }

    public static IMatcher<Integer> priority() {
        return M.string(s -> s.getValue().length());
    }

}