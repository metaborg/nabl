package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.statix.modular.util.TOverrides.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.metaborg.util.functions.Function1;
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
import com.google.common.collect.ListMultimap;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.unification.IUnifier;
import mb.statix.constraints.CConj;
import mb.statix.constraints.Constraints;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.spec.IRule;
import mb.statix.solver.persistent.Solver;
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
        final Optional<? extends ITerm> result = _call(env, term, terms);
        return result.map(strategoTerms::toStratego);
    }
    
    protected Optional<? extends ITerm> _call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        try {
            return call(env, term, terms);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    protected abstract Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException;

    ///////////////////////////////////////
    // Helper methods for checking specs //
    ///////////////////////////////////////

    protected void reportOverlappingRules(final Spec spec) {
        final ListMultimap<String, IRule> overlappingRules = spec.overlappingRules();
        if(!overlappingRules.isEmpty()) {
            logger.error("+-------------------------+");
            logger.error("| FOUND OVERLAPPING RULES |");
            logger.error("+-------------------------+");
            for(Map.Entry<String, Collection<IRule>> entry : overlappingRules.asMap().entrySet()) {
                logger.error("| Overlapping rules for: {}", entry.getKey());
                for(IRule rule : entry.getValue()) {
                    logger.error("| * {}", rule);
                }
            }
            logger.error("+-------------------------+");
        }
    }

    protected IDebugContext getDebugContext(ITerm levelTerm) throws InterpreterException {
        if (OVERRIDE_LOGLEVEL) levelTerm = B.newString(LOGLEVEL);
        
        final String levelString =
                M.stringValue().match(levelTerm).orElseThrow(() -> new InterpreterException("Expected log level."));
        final @Nullable Level level = levelString.equalsIgnoreCase("None") ? null : Level.parse(levelString);
        final IDebugContext debug = level != null ? new LoggerDebugContext(logger, level) : new NullDebugContext();
        return debug;
    }

    ////////////////////////////////////////////////
    // Helper methods for creating error messages //
    ////////////////////////////////////////////////

    protected ITerm makeMessage(String prefix, IConstraint constraint, IUnifier unifier) {
        final ITerm astTerm = findClosestASTTerm(constraint, unifier);
        final StringBuilder message = new StringBuilder();
        message.append(prefix).append(": ").append(constraint.toString(Solver.shallowTermFormatter(unifier)))
                .append("\n");
        formatTrace(5, constraint, unifier, message);
        
        //Truncate the message if it is too long
        if (message.length() > 6000) message.setLength(6000);
        
        return B.newTuple(makeOriginTerm(astTerm), B.newString(message.toString()));
    }

    private ITerm findClosestASTTerm(IConstraint constraint, IUnifier unifier) {
        // @formatter:off
        final Function1<IConstraint, Stream<ITerm>> terms = Constraints.cases(
            onConj -> Stream.empty(),
            onEqual -> Stream.empty(),
            onExists -> Stream.empty(),
            onFalse -> Stream.empty(),
            onInequal -> Stream.empty(),
            onNew -> Stream.empty(),
            onPathLt -> Stream.empty(),
            onPathMatch -> Stream.empty(),
            onResolveQuery -> Stream.empty(),
            onTellEdge -> Stream.empty(),
            onTellRel -> Stream.empty(),
            onTermId -> Stream.empty(),
            onTermProperty -> Stream.empty(),
            onTrue -> Stream.empty(),
            onUser -> onUser.args().stream()
        );
        return terms.apply(constraint).map(unifier::findTerm)
                .filter(t -> TermIndex.get(t).isPresent())
                .filter(t -> TermOrigin.get(t).isPresent()) // HACK Ignore terms without origin, such as empty lists
                .findAny()
                .orElseGet(() -> {
                    return constraint.cause().map(cause -> findClosestASTTerm(cause, unifier)).orElse(B.EMPTY_TUPLE);
                });
        // @formatter:on
    }

    private ITerm makeOriginTerm(ITerm term) {
        return B.EMPTY_TUPLE.withAttachments(term.getAttachments());
    }
    
    private static void formatTrace(int depth, @Nullable IConstraint constraint, IUnifier unifier, StringBuilder sb) {
        for (int i = 0; constraint != null && i < depth;) {
            if (!(constraint instanceof CConj)) {
                sb.append("<br>");
                sb.append("&gt;&nbsp;");
                String c = constraint.toString(Solver.shallowTermFormatter(unifier));
                c = c.replace("&", "&amp;");
                c = c.replace("<", "&lt;");
                c = c.replace(">", "&gt;");
                sb.append(c);
                i++;
            }
            constraint = constraint.cause().orElse(null);
        }
    }

    private static void formatTrace(@Nullable IConstraint constraint, IUnifier unifier, StringBuilder sb) {
        while(constraint != null) {
            sb.append("<br>");
            sb.append("&gt;&nbsp;");
            String c = constraint.toString(Solver.shallowTermFormatter(unifier));
            c = c.replace("&", "&amp;");
            c = c.replace("<", "&lt;");
            c = c.replace(">", "&gt;");
            sb.append(c);
            constraint = constraint.cause().orElse(null);
        }
    }

}
