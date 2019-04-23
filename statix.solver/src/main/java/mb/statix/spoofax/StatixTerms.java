package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import mb.nabl2.regexp.IAlphabet;
import mb.nabl2.regexp.IRegExp;
import mb.nabl2.regexp.IRegExpBuilder;
import mb.nabl2.regexp.impl.FiniteAlphabet;
import mb.nabl2.regexp.impl.RegExpBuilder;
import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.impl.Relation;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CInequal;
import mb.statix.solver.constraint.CNew;
import mb.statix.solver.constraint.CPathDst;
import mb.statix.solver.constraint.CPathLabels;
import mb.statix.solver.constraint.CPathLt;
import mb.statix.solver.constraint.CPathMatch;
import mb.statix.solver.constraint.CPathScopes;
import mb.statix.solver.constraint.CPathSrc;
import mb.statix.solver.constraint.CResolveQuery;
import mb.statix.solver.constraint.CTellEdge;
import mb.statix.solver.constraint.CTellRel;
import mb.statix.solver.constraint.CTermId;
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.statix.spec.Type;

public class StatixTerms {

    public static final ITerm QUERY_SCOPES = B.EMPTY_TUPLE;
    public static final ITerm END_OF_PATH = B.newAppl("EOP");
    public static final ITerm DECL_REL = B.newAppl("Decl");

    public static final ITerm SCOPE_SORT = B.newAppl("SCOPE");
    public static final Type SCOPE_REL_TYPE = Type.of(ImmutableList.of(SCOPE_SORT), ImmutableList.of());

    public static final String SCOPE_OP = "Scope";
    public static final String OCCURRENCE_OP = "Occurrence";
    public static final String SCOPEID_OP = "ScopeId";
    public static final String TERMID_OP = "TermId";
    public static final String NOID_OP = "NoId";

    public static IMatcher<Spec> spec() {
        return IMatcher.flatten(M.tuple4(M.req(labels()), M.req(relationDecls()), M.term(), M.req(scopeExtensions()),
                (t, labels, relations, rulesTerm, ext) -> {
                    Optional<ListMultimap<String, Rule>> maybeRules = M.req(rules(labels)).match(rulesTerm);
                    return maybeRules.map(rules -> {
                        return Spec.of(rules, labels, END_OF_PATH, relations, ext);
                    });
                }));
    }

    public static IMatcher<ListMultimap<String, Rule>> rules(IAlphabet<ITerm> labels) {
        return M.listElems(M.req(rule(labels))).map(rules -> {
            final ImmutableListMultimap.Builder<String, Rule> builder =
                    ImmutableListMultimap.<String, Rule>builder().orderValuesBy(Rule.leftRightPatternOrdering);
            rules.stream().forEach(rule -> {
                builder.put(rule.name(), rule);
            });
            return builder.build();
        });
    }

    public static IMatcher<Rule> rule(IAlphabet<ITerm> labels) {
        return M.appl3("Rule", head(), M.listElems(varTerm()), constraints(labels), (r, h, bvs, bc) -> {
            return Rule.of(h._1(), h._2(), bvs, bc);
        });
    }

    public static IMatcher<Tuple2<String, List<Pattern>>> head() {
        return M.appl2("C", constraintName(), M.listElems(pattern()), (h, name, patterns) -> {
            return ImmutableTuple2.of(name, patterns);
        });
    }

    public static IMatcher<List<IConstraint>> constraints(IAlphabet<ITerm> labels) {
        return (t, u) -> {
            final ImmutableList.Builder<IConstraint> constraints = ImmutableList.builder();
            return M.casesFix(m -> Iterables2.from(
            // @formatter:off
                M.appl2("CConj", m, m, (c, t1, t2) -> {
                    return Unit.unit;
                }),
                M.appl0("CTrue", (c) -> {
                    return Unit.unit;
                }),
                M.appl0("CFalse", (c) -> {
                    constraints.add(new CFalse());
                    return Unit.unit;
                }),
                M.appl2("CEqual", term(), term(), (c, t1, t2) -> {
                    constraints.add(new CEqual(t1, t2));
                    return Unit.unit;
                }),
                M.appl2("CInequal", term(), term(), (c, t1, t2) -> {
                    constraints.add(new CInequal(t1, t2));
                    return Unit.unit;
                }),
                M.appl1("CNew", M.listElems(term()), (c, ts) -> {
                    constraints.add(new CNew(ts));
                    return Unit.unit;
                }),
                M.appl2("CTermId", term(), term(), (c, t1, t2) -> {
                    constraints.add(new CTermId(t1, t2));
                    return Unit.unit;
                }),
                M.appl3("CTellEdge", term(), label(), term(), (c, sourceScope, label, targetScope) -> {
                    constraints.add(new CTellEdge(sourceScope, label, targetScope));
                    return Unit.unit;
                }),
                M.appl3("CTellRel", relation(), M.listElems(term()), term(), (c, rel, args, scope) -> {
                    constraints.add(new CTellRel(scope, rel, args));
                    return Unit.unit;
                }),
                M.appl5("CResolveQuery", queryTarget(), queryFilter(labels), queryMin(labels), term(), term(),
                        (c, rel, filter, min, scope, result) -> {
                    constraints.add(new CResolveQuery(rel, filter, min, scope, result));
                    return Unit.unit;
                }),
                M.appl2("CPathMatch", labelRE(new RegExpBuilder<>(labels)), listTerm(), (c, re, lbls) -> {
                    constraints.add(new CPathMatch(re, lbls));
                    return Unit.unit;
                }),
                M.appl3("CPathLt", labelLt(), term(), term(), (c, lt, l1, l2) -> {
                    constraints.add(new CPathLt(lt, l1, l2));
                    return Unit.unit;
                }),
                M.appl2("CPathSrc", term(), term(), (c, p, rt) -> {
                    constraints.add(new CPathSrc(p, rt));
                    return Unit.unit;
                }),
                M.appl2("CPathDst", term(), term(), (c, p, rt) -> {
                    constraints.add(new CPathDst(p, rt));
                    return Unit.unit;
                }),
                M.appl2("CPathLabels", term(), term(), (c, p, rt) -> {
                    constraints.add(new CPathLabels(p, rt));
                    return Unit.unit;
                }),
                M.appl2("CPathScopes", term(), term(), (c, p, rt) -> {
                    constraints.add(new CPathScopes(p, rt));
                    return Unit.unit;
                }),
                M.appl2("C", constraintName(), M.listElems(term()), (c, name, args) -> {
                    constraints.add(new CUser(name, args));
                    return Unit.unit;
                }),
                M.term(c -> {
                    throw new IllegalArgumentException("Unknown constraint: " + c);
                })
                // @formatter:on
            )).match(t, u).map(v -> constraints.build());
        };
    }

    private static IMatcher<String> constraintName() {
        // @formatter:off
        return M.cases(
            M.stringValue(),
            M.appl().filter(t -> t.getArity() == 0).map(t -> t.getOp())
        );
        // @formatter:on
    }

    public static IMatcher<Optional<ITerm>> queryTarget() {
        // @formatter:off
        return M.cases(
            M.appl0("NoTarget", t -> Optional.empty()),
            M.appl1("RelTarget", relation(), (t, rel) -> Optional.of(rel))
        );
        // @formatter:on
    }

    public static IMatcher<IQueryFilter> queryFilter(IAlphabet<ITerm> labels) {
        return M.appl2("Filter", hoconstraint(labels), hoconstraint(labels), (f, pathConstraint, dataConstraint) -> {
            return new QueryFilter(pathConstraint, dataConstraint);
        });
    }

    public static IMatcher<IQueryMin> queryMin(IAlphabet<ITerm> labels) {
        return M.appl2("Min", hoconstraint(labels), hoconstraint(labels), (m, pathConstraint, dataConstraint) -> {
            return new QueryMin(pathConstraint, dataConstraint);
        });
    }

    public static IMatcher<Rule> hoconstraint(IAlphabet<ITerm> labels) {
        return M.appl3("LLam", M.listElems(pattern()), M.listElems(varTerm()), constraints(labels),
                (t, ps, vs, c) -> Rule.of("", ps, vs, c));
    }

    public static IMatcher<Map<ITerm, Type>> relationDecls() {
        return M.listElems(relationDecl()).map(relDecls -> {
            final ImmutableMap.Builder<ITerm, Type> builder = ImmutableMap.builder();
            builder.put(DECL_REL, Type.of(ImmutableList.of(B.newTuple()), ImmutableList.of()));
            relDecls.stream().forEach(relDecl -> {
                builder.put(relDecl._1(), relDecl._2());
            });
            return builder.build();
        });
    }

    public static IMatcher<Tuple2<ITerm, Type>> relationDecl() {
        return M.tuple2(M.appl1("Rel", M.string()), type(), (rd, rel, type) -> ImmutableTuple2.of(rel, type));
    }

    public static IMatcher<Type> type() {
        // @formatter:off
        return M.cases(
            M.appl1("SimpleType", M.listElems(), (ty, intys) -> Type.of(intys, Collections.emptyList())),
            M.appl2("FunType", M.listElems(), M.listElems(), (ty, intys, outtys) -> Type.of(intys, outtys))
        );
        // @formatter:on
    }

    public static IMatcher<ITerm> relation() {
        // @formatter:off
        return M.cases(
            M.appl0("Decl"),
            M.appl1("Rel", M.string())
        );
        // @formatter:on
    }

    public static IMatcher<Multimap<String, Tuple2<Integer, ITerm>>> scopeExtensions() {
        return M.listElems(scopeExtension(), (t, exts) -> {
            final ImmutableMultimap.Builder<String, Tuple2<Integer, ITerm>> extmap = ImmutableMultimap.builder();
            exts.forEach(ext -> ext.apply(extmap::put));
            return extmap.build();
        });
    }

    public static IMatcher<Tuple2<String, Tuple2<Integer, ITerm>>> scopeExtension() {
        return M.tuple3(M.stringValue(), M.integerValue(), M.term(),
                (t, c, i, lbl) -> ImmutableTuple2.of(c, ImmutableTuple2.of(i - 1, lbl)));
    }

    public static IMatcher<IAlphabet<ITerm>> labels() {
        return M.listElems(label(), (t, ls) -> new FiniteAlphabet<>(ls));
    }

    // @formatter:off
    private static final IMatcher<ITerm> LABEL_MATCHER =
            M.cases(
                M.appl0("EOP"),
                M.appl1("Label", M.string())
            );
    public static IMatcher<ITerm> label() {
        return LABEL_MATCHER;
    }
    // @formatter:on

    private static IMatcher<IRegExp<ITerm>> labelRE(IRegExpBuilder<ITerm> builder) {
        // @formatter:off
        return M.casesFix(m -> Iterables2.from(
            M.appl0("Empty", (t) -> builder.emptySet()),
            M.appl0("Epsilon", (t) -> builder.emptyString()),
            M.appl1("Closure", m, (t, re) -> builder.closure(re)),
            M.appl1("Neg", m, (t, re) -> builder.complement(re)),
            M.appl2("Concat", m, m, (t, re1, re2) -> builder.concat(re1, re2)),
            M.appl2("And", m, m, (t, re1, re2) -> builder.and(re1, re2)),
            M.appl2("Or", m, m, (t, re1, re2) -> builder.or(re1, re2)),
            label().map(l -> builder.symbol(l))
        ));
        // @formatter:on
    }

    public static IMatcher<IRelation.Immutable<ITerm>> labelLt() {
        return M.listElems(labelPair(), (t, ps) -> {
            final IRelation.Transient<ITerm> order = Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
            for(Tuple2<ITerm, ITerm> p : ps) {
                try {
                    order.add(p._1(), p._2());
                } catch(RelationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return order.freeze();
        });
    }

    public static IMatcher<Tuple2<ITerm, ITerm>> labelPair() {
        return M.appl2("LabelPair", label(), label(), (t, l1, l2) -> ImmutableTuple2.of(l1, l2));
    }

    public static IMatcher<ITerm> term() {
        // @formatter:off
        return M.<ITerm>casesFix(m -> Iterables2.from(
            varTerm(),
            M.appl2("Op", M.stringValue(), M.listElems(m), (t, op, args) -> {
                return B.newAppl(op, args, t.getAttachments());
            }),
            M.appl1("Tuple", M.listElems(m), (t, args) -> {
                return B.newTuple(args, t.getAttachments());
            }),
            M.appl1("Str", M.stringValue(), (t, string) -> {
                return B.newString(string, t.getAttachments());
            }),
            M.appl1("Int", M.stringValue(), (t, integer) -> {
                return B.newInt(Integer.parseInt(integer), t.getAttachments());
            }),
            listTerm(),
            // SCOPE_OP -- has no syntax
            // SCOPEID_OP -- has no syntax
            // TERMID_OP -- has no syntax
            // NOID_OP -- has no syntax
            M.appl3(OCCURRENCE_OP, M.string(), M.listElems(m), positionTerm(), (t, ns, args, pos) -> {
                List<ITerm> applArgs = ImmutableList.of(ns, B.newList(args), pos);
                return B.newAppl(OCCURRENCE_OP, applArgs, t.getAttachments());
            })
        ));
        // @formatter:on
    }

    private static IMatcher<ITerm> positionTerm() {
        // @formatter:off
        return M.cases(
            M.appl0("NoId"),
            varTerm()
        );
        // @formatter:on
    }

    public static IMatcher<IListTerm> listTerm() {
        // @formatter:off
        return M.casesFix(m -> Iterables2.from(
            varTerm(),
            M.appl1("List", M.listElems((t, u) -> term().match(t, u)), (t, elems) -> {
                final List<ImmutableClassToInstanceMap<Object>> as = Lists.newArrayList();
                elems.stream().map(ITerm::getAttachments).forEach(as::add);
                as.add(t.getAttachments());
                return B.newList(elems, as);
            }),
            M.appl2("ListTail", M.listElems((t, u) -> term().match(t, u)), m, (t, elems, tail) -> {
                final List<ImmutableClassToInstanceMap<Object>> as = Lists.newArrayList();
                elems.stream().map(ITerm::getAttachments).forEach(as::add);
                return B.newListTail(elems, tail, as).withAttachments(t.getAttachments());
            })
        ));
        // @formatter:on
    }

    public static IMatcher<ITermVar> varTerm() {
        return M.preserveAttachments(M.appl1("Var", M.stringValue(), (t, name) -> {
            return B.newVar("", name).withAttachments(t.getAttachments());
        }));
    }

    public static IMatcher<Pattern> pattern() {
        // @formatter:off
        return M.<Pattern>casesFix(m -> Iterables2.from(
            varPattern(),
            M.appl2("As", varOrWld(), m, (t, var, pattern) -> {
                return var.map(v -> P.newAs(v, pattern)).orElseGet(() -> P.newAs(pattern));
            }),
            M.appl2("Op", M.stringValue(), M.listElems(m), (t, op, args) -> {
                return P.newAppl(op, args);
            }),
            M.appl1("Tuple", M.listElems(M.req(m)), (t, args) -> {
                return P.newTuple(args);
            }),
            M.appl1("List", M.listElems((t, u) -> m.match(t, u)), (t, elems) -> {
                return P.newList(elems);
            }),
            M.appl2("ListTail", M.listElems((t, u) -> m.match(t, u)), m, (t, elems, tail) -> {
                return P.newListTail(elems, tail);
            }),
            M.appl1("Str", M.stringValue(), (t, string) -> {
                return P.newString(string);
            }),
            M.appl1("Int", M.stringValue(), (t, integer) -> {
                return P.newInt(Integer.parseInt(integer));
            }),
            M.appl3(OCCURRENCE_OP, M.stringValue(), M.listElems(m), positionPattern(), (t, ns, args, pos) -> {
                List<Pattern> applArgs = ImmutableList.of(P.newString(ns), P.newList(args), pos);
                return P.newAppl(OCCURRENCE_OP, applArgs);
            })
        ));
        // @formatter:on
    }

    public static IMatcher<Optional<ITermVar>> varOrWld() {
        // @formatter:off
        return M.cases(
            M.appl0("Wld", (t) -> {
                return Optional.empty();
            }),
            M.appl1("Var", M.stringValue(), (t, name) -> {
                return Optional.of(B.newVar("", name));
            })
        );
        // @formatter:on
    }

    public static IMatcher<Pattern> varPattern() {
        return varOrWld().map(v -> v.map(P::newVar).orElse(P.newWld()));
    }

    private static IMatcher<Pattern> positionPattern() {
        // @formatter:off
        return M.cases(
            M.appl0("NoId", t -> P.newWld()),
            varPattern()
        );
        // @formatter:on
    }

    public static ITerm explicate(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                switch(appl.getOp()) {
                    case SCOPE_OP:
                    case SCOPEID_OP:
                    case TERMID_OP:
                    case NOID_OP: {
                        return B.newAppl(appl.getOp(), appl.getArgs());
                    }
                    case OCCURRENCE_OP: {
                        final ITerm ns = appl.getArgs().get(0);
                        final List<? extends ITerm> args = M.listElems().map(ts -> explicate(ts)).match(appl.getArgs().get(1))
                                .orElseThrow(() -> new IllegalArgumentException());
                        final ITerm pos = explicate(appl.getArgs().get(2));
                        return B.newAppl(appl.getOp(), ns, B.newList(args), pos);
                    }
                    default: {
                        final List<ITerm> args = explicate(appl.getArgs());
                        return B.newAppl("Op", B.newString(appl.getOp()), B.newList(args));
                    }
                }
            },
            list -> explicate(list),
            string -> B.newAppl("Str", string),
            integer -> B.newAppl("Int", B.newString(integer.toString())),
            blob -> B.newString(blob.toString()),
            var -> explicate(var)
        )).withAttachments(term.getAttachments());
        // @formatter:on
    }

    private static ITerm explicate(IListTerm list) {
        // @formatter:off
        final List<ITerm> terms = Lists.newArrayList();
        final List<ImmutableClassToInstanceMap<Object>> attachments = Lists.newArrayList();
        final Ref<ITerm> varTail = new Ref<>();
        while(list != null) {
            list = list.match(ListTerms.cases(
                cons -> {
                    terms.add(explicate(cons.getHead()));
                    attachments.add(cons.getAttachments());
                    return cons.getTail();
                },
                nil -> {
                    attachments.add(nil.getAttachments());
                    return null;
                },
                var -> {
                    varTail.set(explicate(var));
                    attachments.add(ImmutableClassToInstanceMap.builder().build());
                    return null;
                }
            ));
            // @formatter:on
        }
        list = B.newList(terms, attachments);
        if(varTail.get() != null) {
            return B.newAppl("ListTail", list, varTail.get());
        } else {
            return B.newAppl("List", list);
        }
    }

    private static ITerm explicate(ITermVar var) {
        return B.newAppl("Var", Arrays.asList(B.newString(var.getName())));
    }

    private static List<ITerm> explicate(Iterable<? extends ITerm> terms) {
        return Iterables2.stream(terms).map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
    }

    public static IListTerm explicateList(Iterable<? extends ITerm> terms) {
        return B.newList(explicate(terms));
    }

    public static IListTerm
            explicateMapEntries(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries) {
        return B.newList(Iterables2.stream(entries).map(e -> B.newTuple(explicate(e.getKey()), explicate(e.getValue())))
                .collect(ImmutableList.toImmutableList()));
    }
}