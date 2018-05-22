package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Set;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.unit.Unit;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Rule;
import mb.statix.solver.constraint.CEqual;
import mb.statix.solver.constraint.CFalse;
import mb.statix.solver.constraint.CTrue;
import mb.statix.solver.constraint.CUser;

public class StatixTerms {
    private static final ILogger logger = LoggerUtils.logger(StatixTerms.class);

    public static IMatcher<Multimap<String, Rule>> rules() {
        return M.listElems(rule()).map(rules -> {
            final ImmutableMultimap.Builder<String, Rule> builder = ImmutableMultimap.builder();
            rules.stream().forEach(rule -> {
                builder.put(rule.getName(), rule);
            });
            return builder.build();
        });
    }

    public static IMatcher<Rule> rule() {
        return M.appl5("Rule", head(), M.listElems(var()), constraints(), M.listElems(var()), constraints(),
                (r, h, gvs, gc, bvs, bc) -> {
                    return new Rule(h._1(), h._2(), gvs, gc, bvs, bc);
                });
    }

    public static IMatcher<Tuple2<String, List<ITermVar>>> head() {
        return M.appl2("C", M.stringValue(), M.listElems(var()), (h, name, params) -> {
            return ImmutableTuple2.of(name, params);
        });
    }

    public static IMatcher<Set<IConstraint>> constraints() {
        return (t, u) -> {
            final ImmutableSet.Builder<IConstraint> constraints = ImmutableSet.builder();
            return M.casesFix(m -> Iterables2.from(
            // @formatter:off
                M.appl2("CConj", m, m, (c, t1, t2) -> {
                    return Unit.unit;
                }),
                M.appl0("CTrue", (c) -> {
                    constraints.add(new CTrue());
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
                    constraints.add(new CEqual(t1, t2));
                    return Unit.unit;
                }),
                M.appl2("C", M.stringValue(), M.listElems(term()), (c, name, args) -> {
                    constraints.add(new CUser(name, args));
                    return Unit.unit;
                }),
                M.term(c -> {
                    logger.warn("Ignoring constraint {}", c);
                    return Unit.unit;
                })
                // @formatter:on
            )).match(t, u).map(v -> constraints.build());
        };
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
            M.appl1("List", M.listElems(m), (t, elems) -> {
                return B.newList(elems);
            }),
            M.appl2("ListTail", M.listElems(m), m, (t, elems, tail) -> {
                return B.newListTail(elems, (IListTerm) tail);
            }),
            M.appl1("Str", M.string(), (t, string) -> {
                return string;
            }),
            M.appl1("Int", M.integer(), (t, integer) -> {
                return integer;
            }),
            M.term(t -> {
                throw new IllegalArgumentException("Unknown term " + t);
            })
            // @formatter:on
        ));
    }

    public static IMatcher<ITermVar> var() {
        return M.appl1("Var", M.stringValue(), (t, name) -> {
            return B.newVar("", name);
        });
    }

}