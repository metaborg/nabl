package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;

import mb.nabl2.stratego.StrategoTerms;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.IUnifier;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.Constraints;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public abstract class StatixPrimitive extends AbstractPrimitive {
    private static final ILogger logger = LoggerUtils.logger(StatixPrimitive.class);

    final protected int tvars;

    public StatixPrimitive(String name) {
        this(name, 0);
    }

    public StatixPrimitive(String name, int tvars) {
        super(name, 0, tvars);
        this.tvars = tvars;
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        return call(env, env.current(), termArgs, env.getFactory()).map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    private final Optional<? extends IStrategoTerm> call(IContext env, IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        if(sterms.size() != tvars) {
            throw new InterpreterException("Expected " + tvars + " term arguments, but got " + sterms.size());
        }
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final ITerm term = strategoTerms.fromStratego(sterm);
        final List<ITerm> terms =
                sterms.stream().map(strategoTerms::fromStratego).collect(ImmutableList.toImmutableList());
        final Optional<? extends ITerm> result = call(env, term, terms);
        return result.map(strategoTerms::toStratego);
    }

    protected abstract Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException;

    ///////////////////////////////////////
    // Helper methods for checking specs //
    ///////////////////////////////////////

    protected void reportOverlappingRules(final Spec spec) {
        final ListMultimap<String, Rule> overlappingRules = spec.overlappingRules();
        if(!overlappingRules.isEmpty()) {
            logger.error("+-------------------------+");
            logger.error("| FOUND OVERLAPPING RULES |");
            logger.error("+-------------------------+");
            for(Map.Entry<String, Collection<Rule>> entry : overlappingRules.asMap().entrySet()) {
                logger.error("| Overlapping rules for: {}", entry.getKey());
                for(Rule rule : entry.getValue()) {
                    logger.error("| * {}", rule);
                }
            }
            logger.error("+-------------------------+");
        }
    }

    protected IDebugContext getDebugContext(ITerm levelTerm) throws InterpreterException {
        final String levelString =
                M.stringValue().match(levelTerm).orElseThrow(() -> new InterpreterException("Expected log level."));
        final @Nullable Level level = levelString.equalsIgnoreCase("None") ? null : Level.parse(levelString);
        final IDebugContext debug = level != null ? new LoggerDebugContext(logger, level) : new NullDebugContext();
        return debug;
    }

    ////////////////////////////////////////////
    // Helper methods for top-level variables //
    ////////////////////////////////////////////

    /**
     * Create fresh variables for top-level variables, and return updated state and substitution.
     */
    protected Tuple2<ISubstitution.Immutable, State> freshenToplevelVariables(Iterable<ITermVar> vars, State state) {
        final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
        for(ITermVar var : vars) {
            final Tuple2<ITermVar, State> var_state = state.freshVar(var.getName());
            state = var_state._2();
            subst.put(var, var_state._1());
            subst.put(var_state._1(), var);
        }
        return ImmutableTuple2.of(subst.freeze(), state);
    }

    /**
     * Create a substitution for the original top-level variables.
     * 
     * The returned Map preserves iteration order of vars.
     */
    protected Map<ITermVar, ITerm> toplevelSubstitution(Iterable<ITermVar> vars, ISubstitution.Immutable subst,
            State state) {
        final ImmutableMap.Builder<ITermVar, ITerm> vsubst = ImmutableMap.builder();
        for(ITermVar var : vars) {
            final ITerm key = subst.apply(var);
            final ITerm value = state.unifier().findRecursive(key);
            final ITerm varTerm = subst.apply(value);
            if(!var.equals(varTerm)) {
                vsubst.put(var, varTerm);
            }
        }
        return vsubst.build();
    }

    ////////////////////////////////////////////////
    // Helper methods for creating error messages //
    ////////////////////////////////////////////////

    protected ITerm makeMessage(String prefix, IConstraint constraint, IUnifier.Immutable unifier) {
        final ITerm astTerm = findClosestASTTerm(constraint, unifier);
        final StringBuilder message = new StringBuilder();
        message.append(prefix).append(": ").append(constraint.toString(Solver.shallowTermFormatter(unifier)))
                .append("\n");
        formatTrace(constraint, unifier, message);
        return B.newTuple(makeOriginTerm(astTerm), B.newString(message.toString()));
    }

    private ITerm findClosestASTTerm(IConstraint constraint, IUnifier unifier) {
        // @formatter:off
        final Function1<IConstraint, Collection<ITerm>> terms = Constraints.cases(
            onEqual -> ImmutableList.of(),
            onFalse -> ImmutableList.of(),
            onInequal -> ImmutableList.of(),
            onNew -> ImmutableList.of(),
            onPathLt -> ImmutableList.of(),
            onPathMatch -> ImmutableList.of(),
            onResolveQuery -> ImmutableList.of(),
            onTellEdge -> ImmutableList.of(),
            onTellRel -> ImmutableList.of(),
            onTermId -> ImmutableList.of(),
            onTrue -> ImmutableList.of(),
            onUser -> onUser.args()
        );
        // @formatter:on
        return Iterables2.stream(terms.apply(constraint)).map(unifier::findTerm)
                .filter(t -> TermIndex.get(t).isPresent()).findAny().orElseGet(() -> {
                    return constraint.cause().map(cause -> findClosestASTTerm(cause, unifier)).orElse(B.EMPTY_TUPLE);
                });
    }

    private ITerm makeOriginTerm(ITerm term) {
        return B.EMPTY_TUPLE.withAttachments(term.getAttachments());
    }

    private static void formatTrace(@Nullable IConstraint constraint, IUnifier.Immutable unifier, StringBuilder sb) {
        while(constraint != null) {
            sb.append("<br>");
            sb.append("&gt;&nbsp;");
            String c = constraint.toString(Solver.shallowTermFormatter(unifier));
            c = c.replaceAll("&", "&amp;");
            c = c.replaceAll("<", "&lt;");
            c = c.replaceAll(">", "&gt;");
            sb.append(c);
            constraint = constraint.cause().orElse(null);
        }
    }

}