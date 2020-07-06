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
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
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
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.Tuple2;
import mb.statix.arithmetic.ArithTerms;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CAstProperty.Op;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTellRel;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.IMessagePart;
import mb.statix.constraints.messages.Message;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.TermPart;
import mb.statix.constraints.messages.TextPart;
import mb.statix.scopegraph.path.IResolutionPath;
import mb.statix.scopegraph.path.IScopePath;
import mb.statix.scopegraph.path.IStep;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.query.IQueryFilter;
import mb.statix.solver.query.IQueryMin;
import mb.statix.solver.query.QueryFilter;
import mb.statix.solver.query.QueryMin;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleSet;
import mb.statix.spec.Spec;

public class StatixTerms {

    private static final ILogger log = LoggerUtils.logger(StatixTerms.class);

    public static final String SCOPE_OP = "Scope";
    public static final String TERMINDEX_OP = "TermIndex";
    public static final String OCCURRENCE_OP = "StxOccurrence";
    public static final String PATH_EMPTY_OP = "PathEmpty";
    public static final String PATH_STEP_OP = "PathStep";
    public static final String WITHID_OP = "WithId";
    public static final String NOID_OP = "NoId";

    public static IMatcher<Spec> spec() {
        return M.appl5("Spec", M.req(labels()), M.req(labels()), M.term(), rules(), M.req(scopeExtensions()),
                (t, edgeLabels, relationLabels, noRelationLabel, rules, ext) -> {
                    final IAlphabet<ITerm> labels = new FiniteAlphabet<>(
                            Iterables2.cons(noRelationLabel, Iterables.concat(relationLabels, edgeLabels)));
                    return Spec.of(rules, edgeLabels, relationLabels, noRelationLabel, labels, ext);
                });
    }

    public static IMatcher<RuleSet> rules() {
        return M.listElems(M.req(rule())).map(RuleSet::of);
    }

    public static IMatcher<Rule> rule() {
        // @formatter:off
        return M.cases(
            M.appl3("Rule", ruleName(), head(), constraint(), (r, n, h, bc) -> {
                return Rule.of(h._1(), h._2(), bc).withLabel(n);
            }),
            // DEPRECATED
            M.appl4("Rule", ruleName(), head(), M.listElems(varTerm()), constraint(), (r, n, h, bvs, bc) -> {
                log.warn("Rules with explicit local variables are deprecated.");
                return Rule.of(h._1(), h._2(), new CExists(bvs, bc)).withLabel(n);
            })
        );
        // @formatter:on
    }

    public static IMatcher<String> ruleName() {
        // @formatter:off
        return M.<String>cases(
            M.appl0("NoName", (t) -> ""),
            M.appl1("Name", M.stringValue(), (t, n) -> n)
        );
        // @formatter:on
    }

    public static IMatcher<Tuple2<String, List<Pattern>>> head() {
        return M.appl2("C", constraintName(), M.listElems(pattern()), (h, name, patterns) -> {
            return Tuple2.of(name, patterns);
        });
    }

    public static IMatcher<IConstraint> constraint() {
        return (t, u) -> {
            // @formatter:off
            return M.<IConstraint>casesFix(m -> Iterables2.from(
                M.appl4("CArith", ArithTerms.matchExpr(), ArithTerms.matchTest(), ArithTerms.matchExpr(), message(), (c, ae1, op, ae2, msg) -> {
                    return new CArith(ae1, op, ae2, msg.orElse(null));
                }),
                M.appl2("CAstId", term(), term(), (c, t1, t2) -> {
                    return new CAstId(t1, t2);
                }),
                M.appl4("CAstProperty", term(), label(), propertyOp(), term(), (c, idTerm, property, op, valueTerm) -> {
                    return new CAstProperty(idTerm, property, op, valueTerm);
                }),
                M.appl2("CConj", m, m, (c, c1, c2) -> {
                    return new CConj(c1, c2);
                }),
                M.appl3("CEqual", term(), term(), message(), (c, t1, t2, msg) -> {
                    return new CEqual(t1, t2, msg.orElse(null));
                }),
                M.appl2("CExists", M.listElems(varTerm()), constraint(), (c, vs, body) -> {
                    return new CExists(vs, body);
                }),
                M.appl1("CFalse", message(), (c, msg) -> {
                    return new CFalse(msg.orElse(null));
                }),
                M.appl3("CInequal", term(), term(), message(), (c, t1, t2, msg) -> {
                    return new CInequal(ImmutableSet.of(), t1, t2, msg.orElse(null));
                }),
                M.appl1("CNew", M.listElems(term()), (c, ts) -> {
                    return new CNew(ts);
                }),
                M.appl6("CResolveQuery", M.term(), queryFilter(), queryMin(), term(), term(), message(),
                        (c, rel, filter, min, scope, result, msg) -> {
                    return new CResolveQuery(rel, filter, min, scope, result, msg.orElse(null));
                }),
                M.appl3("CTellEdge", term(), label(), term(), (c, sourceScope, label, targetScope) -> {
                    return new CTellEdge(sourceScope, label, targetScope);
                }),
                M.appl3("CTellRel", label(), M.listElems(term()), term(), (c, rel, args, scope) -> {
                    return new CTellRel(scope, rel, B.newTuple(args, c.getAttachments()));
                }),
                M.appl0("CTrue", (c) -> {
                    return new CTrue();
                }),
                M.appl2("CTry", constraint(), message(), (c, body, msg) -> {
                    return new CTry(body, msg.orElse(null));
                }),
                M.appl3("C", constraintName(), M.listElems(term()), message(), (c, name, args, msg) -> {
                    return new CUser(name, args, msg.orElse(null));
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
        // @formatter:off
        return M.cases(
            M.appl2("LLam", M.listElems(pattern()), constraint(), (t, ps, c) -> {
                return Rule.of("", ps, c);
            }),
            // DEPRECATED
            M.appl3("LLam", M.listElems(pattern()), M.listElems(varTerm()), constraint(), (t, ps, vs, c) -> {
                log.warn("Lambdas with explicit local variables are deprecated.");
                return Rule.of("", ps, new CExists(vs, c));
            })
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
                (t, c, i, lbl) -> Tuple2.of(c, Tuple2.of(i - 1, lbl)));
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
        return M.appl2("LabelPair", label(), label(), (t, l1, l2) -> Tuple2.of(l1, l2));
    }

    public static IMatcher<CAstProperty.Op> propertyOp() {
        // @formatter:off
        return M.cases(
            M.appl0("Add", t -> Op.ADD),
            M.appl0("Set", t -> Op.SET)
        );
        // @formatter:on
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
            intTerm(),
            listTerm(),
            // SCOPE_OP -- has no syntax
            // TERMINDEX_OP -- has no syntax
            // NOID_OP -- has no syntax
            // WITHID_OP -- has no syntax
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

    public static IMatcher<IIntTerm> intTerm() {
        return M.appl1("Int", M.stringValue(), (t, integer) -> {
            return B.newInt(Integer.parseInt(integer), t.getAttachments());
        });
    }

    private static IMatcher<ITerm> positionTerm() {
        // @formatter:off
        return M.cases(
            M.appl0(NOID_OP),
            M.appl1(WITHID_OP, varTerm(), (t, v) -> v)
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
                return P.newAppl(op, args, t.getAttachments());
            }),
            M.appl1("Tuple", M.listElems(M.req(m)), (t, args) -> {
                return P.newTuple(args, t.getAttachments());
            }),
            M.appl1("List", M.listElems((t, u) -> m.match(t, u)), (t, elems) -> {
                return P.newList(elems, t.getAttachments());
            }),
            M.appl2("ListTail", M.listElems((t, u) -> m.match(t, u)), m, (t, elems, tail) -> {
                return P.newListTail(elems, tail, t.getAttachments());
            }),
            M.appl1("Str", M.stringValue(), (t, string) -> {
                return P.newString(string, t.getAttachments());
            }),
            M.appl1("Int", M.stringValue(), (t, integer) -> {
                return P.newInt(Integer.parseInt(integer), t.getAttachments());
            }),
            M.appl3(OCCURRENCE_OP, M.stringValue(), M.listElems(m), positionPattern(), (t, ns, args, pos) -> {
                List<Pattern> applArgs = ImmutableList.of(P.newString(ns), P.newList(args), pos);
                return P.newAppl(OCCURRENCE_OP, applArgs, t.getAttachments());
            }),
            M.appl1(PATH_EMPTY_OP, m, (t, s) -> {
                List<Pattern> applArgs = ImmutableList.of(s);
                return P.newAppl(PATH_EMPTY_OP, applArgs, t.getAttachments());
            }),
            M.appl3(PATH_STEP_OP, m, m, m, (t, p, l, s) -> {
                List<Pattern> applArgs = ImmutableList.of(p, l, s);
                return P.newAppl(PATH_STEP_OP, applArgs, t.getAttachments());
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
                return Optional.of(B.newVar("", name, t.getAttachments()));
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
            M.appl0(NOID_OP, t -> P.newWld()),
            M.appl1(WITHID_OP, varPattern(), (t, p) -> p)
        );
        // @formatter:on
    }

    public static IMatcher<Optional<IMessage>> message() {
        // @formatter:off
        return M.cases(
            M.appl0("NoMessage", t -> Optional.empty()),
            M.appl3("Message", messageKind(), messageContent(), messageOrigin(), (t, kind, content, origin) -> {
                return Optional.of(new Message(kind, content, origin.orElse(null)));
            })
        );
        // @formatter:on
    }

    public static IMatcher<List<IMessagePart>> messageContent() {
        // @formatter:off
        return M.cases(
            M.appl1("Str", M.stringValue(), (t, text) -> ImmutableList.of(new TextPart(text))),
            M.appl1("Formatted", M.listElems(messagePart()), (t, parts) -> parts)
        );
        // @formatter:on
    }

    public static IMatcher<IMessagePart> messagePart() {
        // @formatter:off
        return M.cases(
            M.appl1("Text", M.stringValue(), (t, text) -> new TextPart(text)),
            M.appl1("Term", term(), (t, term) -> new TermPart(term))
        );
        // @formatter:on
    }

    public static IMatcher<MessageKind> messageKind() {
        // @formatter:off
        return M.cases(
            M.appl0("Error", t -> MessageKind.ERROR),
            M.appl0("Warning", t -> MessageKind.WARNING),
            M.appl0("Note", t -> MessageKind.NOTE)
        );
        // @formatter:on
    }

    public static IMatcher<Optional<ITerm>> messageOrigin() {
        // @formatter:off
        return M.cases(
            M.appl0("NoOrigin", t -> Optional.empty()),
            M.appl1("Origin", varTerm(), (t, v) -> Optional.of(v))
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
                    case WITHID_OP:
                    case PATH_EMPTY_OP:
                    case PATH_STEP_OP: {
                        return appl;
                    }
                    case OCCURRENCE_OP: {
                        final ITerm ns = appl.getArgs().get(0);
                        final List<? extends ITerm> args = M.listElems().map(ts -> explicate(ts)).match(appl.getArgs().get(1))
                                .orElseThrow(() -> new IllegalArgumentException());
                        final ITerm pos = explicatePosition(appl.getArgs().get(2));
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

    public static List<ITerm> explicate(Iterable<? extends ITerm> terms) {
        return Iterables2.stream(terms).map(StatixTerms::explicate).collect(ImmutableList.toImmutableList());
    }

    public static IListTerm explicateList(Iterable<? extends ITerm> terms) {
        return B.newList(explicate(terms));
    }

    public static IListTerm explicateMapEntries(Iterable<? extends Map.Entry<? extends ITerm, ? extends ITerm>> entries,
            IUniDisunifier unifier) {
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

    private static ITerm explicatePosition(ITerm pos) {
        return M.appl0(NOID_OP).match(pos).orElse(B.newAppl(WITHID_OP, explicate(pos)));
    }

}