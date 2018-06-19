package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.optionals.Optionals;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
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
import mb.nabl2.relations.terms.Relation;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IGuard;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
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
import mb.statix.solver.constraint.CUser;
import mb.statix.solver.guard.GEqual;
import mb.statix.solver.guard.GInequal;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.NamespaceQuery;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.solver.query.ResolveFilter;
import mb.statix.solver.query.ResolveMin;
import mb.statix.spec.Lambda;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.statix.spec.Type;
import mb.statix.terms.AOccurrence;

public class StatixTerms {
    private static final ILogger logger = LoggerUtils.logger(StatixTerms.class);

    public static ITerm QUERY_SCOPES = B.EMPTY_TUPLE;
    public static ITerm END_OF_PATH = B.newAppl("EOP");
    public static ITerm DECL_REL = B.newAppl("Decl");

    public static ITerm SCOPE_SORT = B.newAppl("SCOPE");
    public static Type SCOPE_REL_TYPE = Type.of(ImmutableList.of(SCOPE_SORT), ImmutableList.of());

    public static IMatcher<Spec> spec() {
        return IMatcher.flatten(M.tuple5(labels(), relationDecls(), M.term(), M.term(), scopeExtensions(),
                (t, labels, relations, queriesTerm, rulesTerm, ext) -> {
                    Optional<Map<String, NamespaceQuery>> maybeQueries = namespaceQueries(labels).match(queriesTerm);
                    Optional<Multimap<String, Rule>> maybeRules = rules(labels).match(rulesTerm);
                    return Optionals.lift(maybeQueries, maybeRules, (queries, rules) -> {
                        return Spec.of(rules, labels, END_OF_PATH, relations, queries, ext);
                    });
                }));
    }

    public static IMatcher<Multimap<String, Rule>> rules(IAlphabet<ITerm> labels) {
        return M.listElems(rule(labels)).map(rules -> {
            final ImmutableMultimap.Builder<String, Rule> builder = ImmutableMultimap.builder();
            rules.stream().forEach(rule -> {
                builder.put(rule.getName(), rule);
            });
            return builder.build();
        });
    }

    public static IMatcher<Rule> rule(IAlphabet<ITerm> labels) {
        return M.appl5("Rule", head(), M.listElems(var()), guards(), M.listElems(var()), constraints(labels),
                (r, h, gvs, gc, bvs, bc) -> {
                    return new Rule(h._1(), h._2(), gvs, gc, bvs, bc);
                });
    }

    public static IMatcher<Tuple2<String, List<ITermVar>>> head() {
        return M.appl2("C", M.stringValue(), M.listElems(var()), (h, name, params) -> {
            return ImmutableTuple2.of(name, params);
        });
    }

    public static IMatcher<Map<String, NamespaceQuery>> namespaceQueries(IAlphabet<ITerm> labels) {
        return M.listElems(namespaceQuery(labels)).map(relDecls -> {
            final ImmutableMap.Builder<String, NamespaceQuery> builder = ImmutableMap.builder();
            relDecls.stream().forEach(relDecl -> {
                builder.put(relDecl._1(), relDecl._2());
            });
            return builder.build();
        });
    }

    public static IMatcher<Tuple2<String, NamespaceQuery>> namespaceQuery(IAlphabet<ITerm> labels) {
        return M.tuple3(M.stringValue(), hoconstraint(labels), hoconstraint(labels), (t, ns, filter, min) -> {
            return ImmutableTuple2.of(ns, new NamespaceQuery(filter, min));
        });
    }

    public static IMatcher<Set<IConstraint>> constraints(IAlphabet<ITerm> labels) {
        return (t, u) -> {
            final ImmutableSet.Builder<IConstraint> constraints = ImmutableSet.builder();
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
                M.appl1("CNew", M.listElems(term()), (c, ts) -> {
                    constraints.add(new CNew(ts));
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
                M.appl2("CPathMatch", labelRE(new RegExpBuilder<>(labels)), list(), (c, re, lbls) -> {
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
                M.appl2("C", M.stringValue(), M.listElems(term()), (c, name, args) -> {
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

    public static IMatcher<Set<IGuard>> guards() {
        return (t, u) -> {
            final ImmutableSet.Builder<IGuard> guards = ImmutableSet.builder();
            return M.casesFix(m -> Iterables2.from(
            // @formatter:off
                M.appl2("CConj", m, m, (c, t1, t2) -> {
                    return Unit.unit;
                }),
                M.appl0("CTrue", (c) -> {
                    return Unit.unit;
                }),
                M.appl2("CEqual", term(), term(), (c, t1, t2) -> {
                    guards.add(new GEqual(t1, t2));
                    return Unit.unit;
                }),
                M.appl2("CInequal", term(), term(), (c, t1, t2) -> {
                    guards.add(new GInequal(t1, t2));
                    return Unit.unit;
                }),
                M.term(c -> {
                    throw new IllegalArgumentException("Unknown guard: " + c);
                })
                // @formatter:on
            )).match(t, u).map(v -> guards.build());
        };
    }

    public static IMatcher<Optional<ITerm>> queryTarget() {
        return M.cases(
        // @formatter:off
            M.appl0("NoTarget", t -> Optional.empty()),
            M.appl1("RelTarget", relation(), (t, rel) -> Optional.of(rel))
            // @formatter:on
        );
    }

    public static IMatcher<IQueryFilter> queryFilter(IAlphabet<ITerm> labels) {
        return M.cases(
        // @formatter:off
            M.appl2("Filter", hoconstraint(labels), hoconstraint(labels), (f, pathConstraint, dataConstraint) -> {
                return new QueryFilter(pathConstraint, dataConstraint);
            }),
            M.appl1("ResolveFilter", term(), (f, ref) -> {
                return new ResolveFilter(ref);
            })
            // @formatter:on
        );
    }

    public static IMatcher<IQueryMin> queryMin(IAlphabet<ITerm> labels) {
        return M.cases(
        // @formatter:off
            M.appl2("Min", hoconstraint(labels), hoconstraint(labels), (m, pathConstraint, dataConstraint) -> {
                return new QueryMin(pathConstraint, dataConstraint);
            }),
            M.appl1("ResolveMin", term(), (m, ref) -> {
                return new ResolveMin(ref);
            })
            // @formatter:on
        );
    }

    public static IMatcher<Lambda> hoconstraint(IAlphabet<ITerm> labels) {
        return M.appl3("LLam", M.listElems(term()), M.listElems(M.var()), constraints(labels),
                (t, ps, vs, c) -> new Lambda(ps, vs, c));
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
        return M.cases(
        // @formatter:off
            M.appl1("SimpleType", M.listElems(), (ty, intys) -> Type.of(intys, Collections.emptyList())),
            M.appl2("FunType", M.listElems(), M.listElems(), (ty, intys, outtys) -> Type.of(intys, outtys))
            // @formatter:on
        );
    }

    public static IMatcher<ITerm> relation() {
        return M.cases(
        // @formatter:off
            M.appl0("Decl"),
            M.appl1("Rel", M.string())
            // @formatter:on
        );
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
                (t, c, i, lbl) -> ImmutableTuple2.of(c, ImmutableTuple2.of(i, lbl)));
    }

    public static IMatcher<IAlphabet<ITerm>> labels() {
        return M.listElems(label(), (t, ls) -> new FiniteAlphabet<>(ls));
    }

    public static IMatcher<ITerm> label() {
        return M.cases(
        // @formatter:off
            M.appl0("EOP"),
            M.appl1("Label", M.string())
            // @formatter:on
        );
    }

    private static IMatcher<IRegExp<ITerm>> labelRE(IRegExpBuilder<ITerm> builder) {
        return M.casesFix(m -> Iterables2.from(
        // @formatter:off
            M.appl0("Empty", (t) -> builder.emptySet()),
            M.appl0("Epsilon", (t) -> builder.emptyString()),
            M.appl1("Closure", m, (t, re) -> builder.closure(re)),
            M.appl1("Neg", m, (t, re) -> builder.complement(re)),
            M.appl2("Concat", m, m, (t, re1, re2) -> builder.concat(re1, re2)),
            M.appl2("And", m, m, (t, re1, re2) -> builder.and(re1, re2)),
            M.appl2("Or", m, m, (t, re1, re2) -> builder.or(re1, re2)),
            label().map(l -> builder.symbol(l))
            // @formatter:on
        ));
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
        return M.casesFix(m -> Iterables2.from(
        // @formatter:off
            var(),
            M.appl2("Op", M.stringValue(), M.listElems(m), (t, op, args) -> {
                return B.newAppl(op, args);
            }),
            M.appl1("Tuple", M.listElems(m), (t, args) -> {
                return B.newTuple(args);
            }),
            M.appl1("Str", M.string(), (t, string) -> {
                return string;
            }),
            M.appl1("Int", M.integer(), (t, integer) -> {
                return integer;
            }),
            list(),
            AOccurrence.matcher(term())
            // @formatter:on
        ));
    }

    public static IMatcher<IListTerm> list() {
        return M.casesFix(m -> Iterables2.from(
        // @formatter:off
            var(),
            M.appl1("List", M.listElems((t, u) -> term().match(t, u)), (t, elems) -> {
                return B.newList(elems);
            }),
            M.appl2("ListTail", M.listElems((t, u) -> term().match(t, u)), m, (t, elems, tail) -> {
                return B.newListTail(elems, tail);
            })
            // @formatter:on
        ));
    }

    public static IMatcher<ITermVar> var() {
        return M.appl1("Var", M.stringValue(), (t, name) -> {
            return B.newVar("", name);
        });
    }

    public static ITerm explicate(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                List<ITerm> args = appl.getArgs().stream().map(arg -> explicate(arg)).collect(Collectors.toList());
                return B.newAppl(appl.getOp(), args);
            },
            list -> explicate(list),
            string -> string,
            integer -> integer,
            blob -> blob,
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

}