package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
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
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CPathLt;
import mb.statix.constraints.CPathMatch;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTellRel;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CUser;
import mb.statix.modular.module.IModuleStringComponent;
import mb.statix.modular.module.ModuleString;
import mb.statix.modular.spec.ModuleBoundary;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.spec.IRule;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class StatixTerms {

    public static final String SCOPE_OP = "Scope";
    public static final String TERMINDEX_OP = "TermIndex";
    public static final String OCCURRENCE_OP = "Occurrence";
    public static final String PATH_EMPTY_OP = "PathEmpty";
    public static final String PATH_STEP_OP = "PathStep";
    public static final String NOID_OP = "NoId";

    public static IMatcher<Spec> spec() {
        return M.tuple5(M.req(labels()), M.req(labels()), M.term(), rules(), M.req(scopeExtensions()),
                (t, edgeLabels, relationLabels, noRelationLabel, rules, ext) -> {
                    final IAlphabet<ITerm> labels = new FiniteAlphabet<>(
                            Iterables2.cons(noRelationLabel, Iterables.concat(relationLabels, edgeLabels)));
                    return Spec.of(rules, edgeLabels, relationLabels, noRelationLabel, labels, ext);
                });
    }

    public static IMatcher<ListMultimap<String, IRule>> rules() {
        return M.listElems(M.req(rule())).map(rules -> {
            final ImmutableListMultimap.Builder<String, IRule> builder =
                    ImmutableListMultimap.<String, IRule>builder().orderValuesBy(Rule.leftRightPatternOrdering);
            rules.stream().forEach(rule -> {
                builder.put(rule.name(), rule);
            });
            return builder.build();
        });
    }

    public static IMatcher<IRule> rule() {
        return M.<IRule>cases(
                M.appl3("Rule", head(), M.listElems(varTerm()), constraint(), (r, h, bvs, bc) -> {
                    return Rule.of(h._1(), h._2(), new CExists(bvs, bc));
                }),
                M.appl4("ModuleRule", head(), moduleString(), M.listElems(varTerm()), constraint(), (r, h, mstr, bvs, bc) -> {
                    return ModuleBoundary.of(h._1(), h._2(), mstr, new CExists(bvs, bc));
                })
        );
    }

    public static IMatcher<Tuple2<String, List<Pattern>>> head() {
        return M.appl2("C", constraintName(), M.listElems(pattern()), (h, name, patterns) -> {
            return ImmutableTuple2.of(name, patterns);
        });
    }
    
    public static IMatcher<ModuleString> moduleString() {
        return M.listElems(IModuleStringComponent.matcher()).map(cs -> ModuleString.of(cs));
    }

    public static IMatcher<IConstraint> constraint() {
        return (t, u) -> {
            // @formatter:off
            return M.<IConstraint>casesFix(m -> Iterables2.from(
                M.appl2("CAstId", term(), term(), (c, t1, t2) -> {
                    return new CAstId(t1, t2);
                }),
                M.appl3("CAstProperty", term(), label(), term(), (c, idTerm, property, valueTerm) -> {
                    return new CAstProperty(idTerm, property, valueTerm);
                }),
                M.appl2("CConj", m, m, (c, c1, c2) -> {
                    return new CConj(c1, c2);
                }),
                M.appl2("CEqual", term(), term(), (c, t1, t2) -> {
                    return new CEqual(t1, t2);
                }),
                M.appl2("CExists", M.listElems(varTerm()), constraint(), (c, vs, body) -> {
                    return new CExists(vs, body);
                }),
                M.appl0("CFalse", (c) -> {
                    return new CFalse();
                }),
                M.appl2("CInequal", term(), term(), (c, t1, t2) -> {
                    return new CInequal(t1, t2);
                }),
                M.appl1("CNew", M.listElems(term()), (c, ts) -> {
                    return new CNew(ts);
                }),
                M.appl3("CPathLt", labelLt(), term(), term(), (c, lt, l1, l2) -> {
                    return new CPathLt(lt, l1, l2);
                }),
                M.appl2("CPathMatch", labelRE(new RegExpBuilder<>()), listTerm(), (c, re, lbls) -> {
                    return new CPathMatch(re, lbls);
                }),
                M.appl5("CResolveQuery", M.term(), queryFilter(), queryMin(), term(), term(),
                        (c, rel, filter, min, scope, result) -> {
                    return new CResolveQuery(rel, filter, min, scope, result);
                }),
                M.appl3("CTellEdge", term(), label(), term(), (c, sourceScope, label, targetScope) -> {
                    return new CTellEdge(sourceScope, label, targetScope);
                }),
                M.appl3("CTellRel", label(), M.listElems(term()), term(), (c, rel, args, scope) -> {
                    return new CTellRel(scope, rel, args);
                }),
                M.appl0("CTrue", (c) -> {
                    return new CTrue();
                }),
                M.appl2("C", constraintName(), M.listElems(term()), (c, name, args) -> {
                    return new CUser(name, args);
                }),
                M.term(c -> {
                    throw new IllegalArgumentException("Unknown constraint: " + c);
                })
            )).match(t, u);
            // @formatter:on
        };
    }

    private static IMatcher<String> constraintName() {
        return M.stringValue();
    }

    public static IMatcher<IQueryFilter> queryFilter() {
        return M.appl2("Filter", labelRE(new RegExpBuilder<>()), hoconstraint(), (f, wf, dataConstraint) -> {
            return new QueryFilter(wf, dataConstraint);
        });
    }

    public static IMatcher<IQueryMin> queryMin() {
        return M.appl2("Min", labelLt(), hoconstraint(), (m, ord, dataConstraint) -> {
            return new QueryMin(ord, dataConstraint);
        });
    }

    public static IMatcher<Rule> hoconstraint() {
        return M.appl3("LLam", M.listElems(pattern()), M.listElems(varTerm()), constraint(),
                (t, ps, vs, c) -> Rule.of("", ps, new CExists(vs, c)));
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

    public static IMatcher<List<ITerm>> labels() {
        return M.listElems(label());
    }

    public static IMatcher<ITerm> label() {
        return M.term();
    }

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
            // TERMINDEX_OP -- has no syntax
            // NOID_OP -- has no syntax
            M.appl3(OCCURRENCE_OP, M.string(), M.listElems(m), positionTerm(), (t, ns, args, pos) -> {
                List<ITerm> applArgs = ImmutableList.of(ns, B.newList(args), pos);
                return B.newAppl(OCCURRENCE_OP, applArgs, t.getAttachments());
            }),
            M.appl1(PATH_EMPTY_OP, term(), (t, s) -> {
                List<ITerm> applArgs = ImmutableList.of(s);
                return B.newAppl(PATH_EMPTY_OP, applArgs, t.getAttachments());
            }),
            M.appl3(PATH_STEP_OP, term(), term(), term(), (t, p, l, s) -> {
                List<ITerm> applArgs = ImmutableList.of(p, l, s);
                return B.newAppl(PATH_STEP_OP, applArgs, t.getAttachments());
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
            }),
            M.appl1(PATH_EMPTY_OP, m, (t, s) -> {
                List<Pattern> applArgs = ImmutableList.of(s);
                return P.newAppl(PATH_EMPTY_OP, applArgs);
            }),
            M.appl3(PATH_STEP_OP, m, m, m, (t, p, l, s) -> {
                List<Pattern> applArgs = ImmutableList.of(p, l, s);
                return P.newAppl(PATH_STEP_OP, applArgs);
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
            varPattern(),
            M.appl1("AstIdOp", varPattern(), (t, v) -> v)
        );
        // @formatter:on
    }

    public static ITerm explicate(ITerm term) {
        // @formatter:off
        return term.match(Terms.cases(
            appl -> {
                switch(appl.getOp()) {
                    case SCOPE_OP:
                    case TERMINDEX_OP:
                    case NOID_OP:
                    case PATH_EMPTY_OP:
                    case PATH_STEP_OP: {
                        return appl;
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

    public static IListTerm explicateMapEntries(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries,
            IUnifier unifier) {
        return B.newList(Iterables2.stream(entries)
                .map(e -> B.newTuple(explicate(e.getKey()), explicate(unifier.findRecursive(e.getValue()))))
                .collect(ImmutableList.toImmutableList()));
    }

    public static ITerm explicate(IResolutionPath<Scope, ITerm, ITerm> path) {
        return B.newTuple(explicate(path.getPath()), /*path.getLabel(),*/ B.newTuple(path.getDatum()));
    }

    public static ITerm explicate(IScopePath<Scope, ITerm> path) {
        ITerm pathTerm = B.newAppl(PATH_EMPTY_OP, path.getSource());
        for(IStep<Scope, ITerm> step : path) {
            pathTerm = B.newAppl(PATH_STEP_OP, pathTerm, step.getLabel(), step.getTarget());
        }
        return pathTerm;
    }

}