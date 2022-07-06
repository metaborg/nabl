package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.metaborg.util.Ref;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.optionals.Optionals;
import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import mb.nabl2.terms.IAttachments;
import mb.nabl2.terms.IIntTerm;
import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.ListTerms;
import mb.nabl2.terms.Terms;
import mb.nabl2.terms.build.Attachments;
import mb.nabl2.terms.matching.Pattern;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.matching.Transform.T;
import mb.nabl2.terms.substitution.FreshVars;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.scopegraph.oopsla20.path.IResolutionPath;
import mb.scopegraph.oopsla20.path.IScopePath;
import mb.scopegraph.oopsla20.path.IStep;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.ScopeGraph;
import mb.scopegraph.regexp.IRegExp;
import mb.scopegraph.regexp.IRegExpBuilder;
import mb.scopegraph.regexp.impl.RegExpBuilder;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.RelationException;
import mb.scopegraph.relations.impl.Relation;
import mb.scopegraph.resolution.RCExp;
import mb.scopegraph.resolution.RExp;
import mb.scopegraph.resolution.RMerge;
import mb.scopegraph.resolution.RResolve;
import mb.scopegraph.resolution.RShadow;
import mb.scopegraph.resolution.RStep;
import mb.scopegraph.resolution.RSubEnv;
import mb.scopegraph.resolution.RVar;
import mb.scopegraph.resolution.State;
import mb.scopegraph.resolution.StateMachine;
import mb.statix.arithmetic.ArithTerms;
import mb.statix.constraints.CArith;
import mb.statix.constraints.CAstId;
import mb.statix.constraints.CAstProperty;
import mb.statix.constraints.CAstProperty.Op;
import mb.statix.constraints.CCompiledQuery;
import mb.statix.constraints.CConj;
import mb.statix.constraints.CEqual;
import mb.statix.constraints.CExists;
import mb.statix.constraints.CFalse;
import mb.statix.constraints.CInequal;
import mb.statix.constraints.CNew;
import mb.statix.constraints.CResolveQuery;
import mb.statix.constraints.CTellEdge;
import mb.statix.constraints.CTrue;
import mb.statix.constraints.CTry;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.IMessagePart;
import mb.statix.constraints.messages.Message;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.TermPart;
import mb.statix.constraints.messages.TextPart;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
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
    public static final String SCOPEGRAPH_OP = "ScopeGraph";

    private static final String EOP_OP = "EOP";

    private static final String STATE_OP = "State";

    private static final String STEP_OP = "Step";

    private static final String RESOLVE_OP = "Resolve";
    private static final String SUBENV_OP = "SubEnv";
    private static final String MERGE_OP = "Merge";
    private static final String SHADOW_OP = "Shadow";
    private static final String CEXP_OP = "CExp";

    private static final String RVAR_OP = "RVar";

    public static IMatcher<Spec> spec() {
        return M.appl5("Spec", M.req(labels()), M.req(labels()), M.term(), rules(), M.req(scopeExtensions()),
                (t, edgeLabels, dataLabels, noRelationLabel, rules, ext) -> {
                    return Spec.of(rules, edgeLabels, dataLabels, ext).precomputeCriticalEdges();
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
                    return Constraints.conjoin(ts.stream().map(s -> new CNew(s, s)).collect(Collectors.toList()));
                }),
                resolveQuery(),
                M.appl3("CPreCompiledQuery", resolveQuery(), states(), M.stringValue(), (c, query, states, initial) -> {
                    final State<ITerm> initialState = states.get(initial);
                    if(initialState == null) {
                        throw new IllegalStateException("Invalid initial state: " + initial);
                    }
                    final StateMachine<ITerm> stateMachine = new StateMachine<>(states, initialState);
                    return new CCompiledQuery(query.filter(), query.min(), query.scopeTerm(), query.resultTerm(), query.message().orElse(null), stateMachine);
                }),
                M.appl3("CTellEdge", term(), label(), term(), (c, sourceScope, label, targetScope) -> {
                    return new CTellEdge(sourceScope, label, targetScope);
                }),
                M.appl3("CTellRel", label(), M.listElems(term()), term(), (c, rel, args, scope) -> {
                    final FreshVars f = new FreshVars(args.stream().flatMap(a -> a.getVars().stream()).collect(Collectors.toList()));
                    final ITermVar d = f.fresh("d");
                    return (IConstraint) Constraints.exists(ImmutableSet.of(d), Constraints.conjoin(Iterables2.from(
                        new CNew(d, B.newTuple(args, c.getAttachments())),
                        new CTellEdge(scope, rel, d)
                    )));
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

    public static IMatcher<CResolveQuery> resolveQuery() {
        // @formatter:off
        return (t, u) -> M.appl6("CResolveQuery", M.term(), M.term(), M.term(), term(), term(), message(),
            (c, rel, filterTerm, minTerm, scope, result, msg) -> {
                final Optional<QueryFilter> maybeFilter = queryFilter(rel).match(filterTerm, u);
                final Optional<QueryMin> maybeMin = queryMin(rel).match(minTerm, u);
                return Optionals.lift(maybeFilter, maybeMin, (filter, min) -> {
                    return new CResolveQuery(filter, min, scope, result, msg.orElse(null));
                });
            }).flatMap(o -> o)
            .match(t, u);
        // @formatter:on
    }

    public static IMatcher<Map<String, State<ITerm>>> states() {
        return M.listElems(state()).map(states -> {
            final ImmutableMap.Builder<String, State<ITerm>> mapBuilder = ImmutableMap.builder();
            for(Tuple2<String, State<ITerm>> state : states) {
                mapBuilder.put(state._1(), state._2());
            }
            return mapBuilder.build();
        });
    }

    public static IMatcher<Tuple2<String, State<ITerm>>> state() {
        return M.appl3(STATE_OP, M.stringValue(), M.listElems(step()), rvar(), (appl, name, steps, var) -> {
            // @formatter:off
            final ImmutableList<RStep<ITerm>> ss = steps.stream()
                    .map(Tuple3::_1)
                    .collect(ImmutableList.toImmutableList());
            final boolean accepting = steps.stream().anyMatch(Tuple3::_2);
            final ImmutableMap<ITerm, String> transitions = steps.stream()
                    .map(Tuple3::_3)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(ImmutableMap.toImmutableMap(Tuple2::_1, Tuple2::_2, (v1, v2) -> {
                        if(!v1.equals(v2)) {
                            throw new IllegalArgumentException("Conflicting states " + v1 + " and " + v2 + ".");
                        }
                        return v1;
                    }));
            // @formatter:off

            return Tuple2.of(name, new State<ITerm>(ss, var, accepting, transitions));
        });
    }

    public static IMatcher<Tuple3<RStep<ITerm>, Boolean, Optional<Tuple2<ITerm, String>>>> step() {
        return M.appl2(STEP_OP, rvar(), rexp(), (appl, var, exp) -> {
            return Tuple3.of(new RStep<>(var, exp._1()), exp._2(), exp._3());
        });
    }

    public static IMatcher<Tuple3<RExp<ITerm>, Boolean, Optional<Tuple2<ITerm, String>>>> rexp() {
        // @formatter:off
        return M.casesFix(m -> Iterables2.from(
            M.appl0(RESOLVE_OP, appl -> Tuple3.of(RResolve.of(), true, Optional.empty())),
            M.appl2(SUBENV_OP, StatixTerms.label(), M.stringValue(), (appl, lbl, state) -> Tuple3.of(new RSubEnv<>(lbl, state), false, Optional.of(Tuple2.<ITerm, String>of(lbl, state)))),
            M.appl1(MERGE_OP, M.listElems(rvar()), (appl, envs) -> Tuple3.of(new RMerge<>(envs), false, Optional.empty())),
            M.appl2(SHADOW_OP, rvar(), rvar(), (appl, left, right) -> Tuple3.of(new RShadow<>(left, right), false, Optional.empty())),
            M.appl2(CEXP_OP, rvar(), m, (appl, env, exp) -> Tuple3.of(new RCExp<>(env, exp._1()), exp._2(), exp._3()))
        ));
        // @formatter:on
    }

    public static IMatcher<RVar> rvar() {
        return M.appl1(RVAR_OP, M.stringValue(), (appl, name) -> new RVar(name));
    }

    private static IMatcher<String> constraintName() {
        return M.stringValue();
    }

    public static IMatcher<QueryFilter> queryFilter(ITerm rel) {
        final IRegExpBuilder<ITerm> builder = new RegExpBuilder<>();
        IMatcher<IRegExp<ITerm>> reMatcher = labelRE(builder);
        if(!isEOP(rel)) {
            // append relation to the regular expression
            reMatcher = reMatcher.map(re -> builder.concat(re, builder.symbol(rel)));
        }
        return M.appl2("Filter", reMatcher, hoconstraint(), (f, wf, dataConstraint) -> {
            return new QueryFilter(wf, dataConstraint);
        });
    }

    public static IMatcher<QueryMin> queryMin(ITerm rel) {
        IMatcher<IRelation.Immutable<EdgeOrData<ITerm>>> ltMatcher = labelLt();
        if(!isEOP(rel)) {
            // patch label order, replacing EOP with rel
            ltMatcher = ltMatcher.map(lt -> {
                final IRelation.Transient<EdgeOrData<ITerm>> newLt = Relation.Transient.of(lt.getDescription());
                lt.stream().forEach(ls -> {
                    final EdgeOrData<ITerm> newL1 = ls._1().match(() -> EdgeOrData.edge(rel), EdgeOrData::edge);
                    final EdgeOrData<ITerm> newL2 = ls._2().match(() -> EdgeOrData.edge(rel), EdgeOrData::edge);
                    try {
                        newLt.add(newL1, newL2);
                    } catch(RelationException e) {
                        // patching should not invalidate the relation
                        // we assume that rel is not currently in the relation
                        throw new IllegalStateException();
                    }
                });
                return newLt.freeze();
            });
        }
        return M.appl2("Min", ltMatcher, hoconstraint(), (m, ord, dataConstraint) -> {
            return new QueryMin(ord, dataConstraint);
        });
    }

    private static boolean isEOP(ITerm label) {
        return M.appl0(EOP_OP).match(label).isPresent();
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

    public static IMatcher<IRelation.Immutable<EdgeOrData<ITerm>>> labelLt() {
        return M.listElems(labelPair(), (t, ps) -> {
            final IRelation.Transient<EdgeOrData<ITerm>> order =
                    Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
            for(Tuple2<EdgeOrData<ITerm>, EdgeOrData<ITerm>> p : ps) {
                try {
                    order.add(p._1(), p._2());
                } catch(RelationException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return order.freeze();
        });
    }

    public static IMatcher<Tuple2<EdgeOrData<ITerm>, EdgeOrData<ITerm>>> labelPair() {
        return M.appl2("LabelPair", label(), label(), (t, l1, l2) -> {
            final EdgeOrData<ITerm> _l1 = isEOP(l1) ? EdgeOrData.data() : EdgeOrData.edge(l1);
            final EdgeOrData<ITerm> _l2 = isEOP(l2) ? EdgeOrData.data() : EdgeOrData.edge(l2);
            return Tuple2.of(_l1, _l2);
        });
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
                return B.newString(Terms.unescapeString(string), t.getAttachments());
            }),
            intTerm(),
            listTerm(),
            M.appl(SCOPE_OP, t -> t),
            M.appl(TERMINDEX_OP, t -> t),
            M.appl(NOID_OP, t -> t),
            M.appl(WITHID_OP, t -> t),
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
                final List<IAttachments> as = Lists.newArrayList();
                elems.stream().map(ITerm::getAttachments).forEach(as::add);
                as.add(t.getAttachments());
                return B.newList(elems, as);
            }),
            M.appl2("ListTail", M.listElems((t, u) -> term().match(t, u)), m, (t, elems, tail) -> {
                final List<IAttachments> as = Lists.newArrayList();
                elems.stream().map(ITerm::getAttachments).forEach(as::add);
                return B.newListTail(elems, tail, as).withAttachments(t.getAttachments());
            })
        ));
        // @formatter:on
    }

    public static IMatcher<ITermVar> varTerm() {
        return M.preserveAttachments(M.appl1("Var", M.stringValue(), (t, name) -> {
            return B.newVar("", name, t.getAttachments());
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
                return P.newString(Terms.unescapeString(string), t.getAttachments());
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
            M.appl1("Str", M.stringValue(), (t, text) -> ImmutableList.of(new TextPart(Terms.unescapeString(text)))),
            M.appl1("Formatted", M.listElems(messagePart()), (t, parts) -> parts)
        );
        // @formatter:on
    }

    public static IMatcher<IMessagePart> messagePart() {
        // @formatter:off
        return M.cases(
            M.appl1("Text", M.stringValue(), (t, text) -> new TextPart(unescapeMessageText(text))),
            M.appl1("Term", term(), (t, term) -> new TermPart(term))
        );
        // @formatter:on
    }

    private static String unescapeMessageText(String text) {
        final StringBuilder sb = new StringBuilder();
        final StringCharacterIterator it = new StringCharacterIterator(text);
        while(it.current() != CharacterIterator.DONE) {
            char c1 = it.current();
            if(c1 == '\\') {
                char c2 = it.next();
                if(c2 != CharacterIterator.DONE) {
                    switch(c2) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '[':
                        case ']':
                        case '\\':
                            sb.append(c2);
                            break;
                        default:
                            sb.append(c1).append(c2);
                            break;
                    }
                } else {
                    sb.append(c1);
                }
            } else {
                sb.append(c1);
            }
            it.next();
        }
        return sb.toString();
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

    ///////////////////////////////////////////////////////////////////////////

    public static IMatcher<IScopeGraph.Immutable<Scope, ITerm, ITerm>> scopeGraph() {
        // @formatter:off
        return M.cases(
            M.appl1(SCOPEGRAPH_OP, scopeGraphEntries(), (t, scopeGraph) -> scopeGraph),
            // DEPRECATED
            scopeGraphEntries()
        );
        // @formatter:on
    }

    public static IMatcher<IScopeGraph.Immutable<Scope, ITerm, ITerm>> scopeGraphEntries() {
        return M.listElems(scopeEntry(), (t, scopeEntries) -> {
            final IScopeGraph.Transient<Scope, ITerm, ITerm> scopeGraph = ScopeGraph.Transient.of();
            for(Tuple3<Scope, Optional<ITerm>, Map<ITerm, List<Scope>>> se : scopeEntries) {
                Scope s = se._1();
                if(se._2().isPresent()) {
                    scopeGraph.setDatum(s, se._2().get());
                }
                for(Entry<ITerm, List<Scope>> ee : se._3().entrySet()) {
                    final ITerm lbl = ee.getKey();
                    for(Scope tgt : ee.getValue()) {
                        scopeGraph.addEdge(s, lbl, tgt);
                    }
                }
            }
            return scopeGraph.freeze();
        });
    }

    public static IMatcher<Tuple3<Scope, Optional<ITerm>, Map<ITerm, List<Scope>>>> scopeEntry() {
        return M.tuple3(Scope.matcher(), M.option(M.term()), M.map(label(), M.listElems(Scope.matcher())),
                (t, scope, maybeDatum, edges) -> {
                    return Tuple3.of(scope, maybeDatum, edges);
                });
    }

    public static ITerm toTerm(IScopeGraph.Immutable<Scope, ITerm, ITerm> scopeGraph, IUnifier.Immutable unifier) {
        final Map<Scope, ITerm> dataEntries = Maps.newHashMap(); // Scope * ITerm
        final Map<Scope, ListMultimap<ITerm, Scope>> edgeEntries = Maps.newHashMap(); // Scope * (Label * Scope)

        scopeGraph.getData().forEach((s, d) -> {
            d = unifier.findRecursive(d);
            dataEntries.put(s, d);
        });

        scopeGraph.getEdges().forEach((src_lbl, tgt) -> {
            ListMultimap<ITerm, Scope> edges = edgeEntries.getOrDefault(src_lbl.getKey(), LinkedListMultimap.create());
            edges.putAll(src_lbl.getValue(), tgt);
            edgeEntries.put(src_lbl.getKey(), edges);
        });

        final List<ITerm> scopeEntries = Lists.newArrayList(); // [Scope * ITerm? * [Label * [Scope]]]
        for(Scope scope : Sets.union(edgeEntries.keySet(), dataEntries.keySet())) {
            final ITerm data = Optional.ofNullable(dataEntries.get(scope)).map(d -> B.newAppl("Some", d))
                    .orElse(B.newAppl("None"));

            final ITerm edges = Optional.ofNullable(edgeEntries.get(scope)).map(es -> {
                final List<ITerm> lblTgts = Lists.newArrayList();
                es.asMap().entrySet().forEach(ee -> {
                    final ITerm lbl_tgt = B.newTuple(ee.getKey(), B.newList(explicateVars(ee.getValue())));
                    lblTgts.add(lbl_tgt);
                });
                return B.newList(lblTgts);
            }).orElse(B.newList());

            scopeEntries.add(B.newTuple(explicateVars(scope), data, edges));
        }

        // @formatter:on
        return B.newAppl(SCOPEGRAPH_OP, B.newList(scopeEntries));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static ITerm explode(ITerm term) {
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
                        final List<? extends ITerm> args = M.listElems().map(ts -> explode(ts)).match(appl.getArgs().get(1))
                                .orElseThrow(() -> new IllegalArgumentException());
                        final ITerm pos = explodePosition(appl.getArgs().get(2));
                        return B.newAppl(appl.getOp(), ImmutableList.of(ns, B.newList(args), pos), term.getAttachments());
                    }
                    default: {
                        final List<ITerm> args = explode(appl.getArgs());
                        return B.newAppl("Op", ImmutableList.of(B.newString(appl.getOp()), B.newList(args)), term.getAttachments());
                    }
                }
            },
            list -> explode(list),
            string -> B.newAppl("Str", ImmutableList.of(B.newString(Terms.escapeString(string.getValue()))), term.getAttachments()),
            integer -> B.newAppl("Int", ImmutableList.of(B.newString(integer.toString())), term.getAttachments()),
            blob -> B.newString(blob.toString(), term.getAttachments()),
            var -> explode(var)
        )).withAttachments(term.getAttachments());
        // @formatter:on
    }

    private static ITerm explode(IListTerm list) {
        // @formatter:off
        final List<ITerm> terms = Lists.newArrayList();
        final List<IAttachments> attachments = Lists.newArrayList();
        final Ref<ITerm> varTail = new Ref<>();
        while(list != null) {
            list = list.match(ListTerms.cases(
                cons -> {
                    terms.add(explode(cons.getHead()));
                    attachments.add(cons.getAttachments());
                    return cons.getTail();
                },
                nil -> {
                    attachments.add(nil.getAttachments());
                    return null;
                },
                var -> {
                    varTail.set(explode(var));
                    attachments.add(Attachments.empty());
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

    public static ITerm explode(ITermVar var) {
        if(!var.getResource().isEmpty()) {
            throw new IllegalArgumentException(
                    "Exploding a variable with a resource is not possible. Exploding a unification variable instead of a syntax variable?");
        }
        return B.newAppl("Var", B.newString(var.getName()));
    }

    private static List<ITerm> explode(Iterable<? extends ITerm> terms) {
        return Iterables2.stream(terms).map(StatixTerms::explode).collect(ImmutableList.toImmutableList());
    }

    private static ITerm explodePosition(ITerm pos) {
        return M.appl0(NOID_OP).match(pos).orElse(B.newAppl(WITHID_OP, explode(pos)));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static ITerm explicateVars(ITerm term) {
        // @formatter:off
        return T.sometd(M.cases(
            M.cons(M.term(), M.var(), (t, hd, tl) -> B.newAppl("Conc", explicateVars(hd), explicateVar(tl))),
            M.var(StatixTerms::explicateVar)
        )::match).apply(term);
        // @formatter:on
    }

    private static ITerm explicateVar(ITermVar var) {
        return B.newAppl("Var", B.newString(var.getResource()), B.newString(var.getName()));
    }

    public static List<ITerm> explicateVars(Iterable<? extends ITerm> terms) {
        return Iterables2.stream(terms).map(StatixTerms::explicateVars).collect(ImmutableList.toImmutableList());
    }

    ///////////////////////////////////////////////////////////////////////////

    public static ITerm pathToTerm(IResolutionPath<Scope, ITerm, ITerm> path, Set<ITerm> relationLabels) {
        return B.newTuple(pathToTerm(path.getPath(), relationLabels), /*path.getLabel(),*/ B.newTuple(path.getDatum()));
    }

    public static ITerm pathToTerm(IScopePath<Scope, ITerm> path, Set<ITerm> relationLabels) {
        ITerm pathTerm = B.newAppl(PATH_EMPTY_OP, path.getSource());
        final Iterator<IStep<Scope, ITerm>> it = path.iterator();
        while(it.hasNext()) {
            final IStep<Scope, ITerm> step = it.next();
            if(relationLabels.contains(step.getLabel())) {
                // drop declaration labels at then end, only retain "edge" steps
                break;
            }
            pathTerm = B.newAppl(PATH_STEP_OP, pathTerm, step.getLabel(), step.getTarget());
        }
        if(it.hasNext()) {
            throw new IllegalArgumentException("Encountered a relation label in the middle of path.");
        }
        return pathTerm;
    }

}
