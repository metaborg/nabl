package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.*;

import jakarta.annotation.Nullable;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public abstract class StatixPrimitive extends AbstractPrimitive {

    protected static final ILogger logger = LoggerUtils.logger(StatixPrimitive.class);
    protected static final String WITH_CONFIG_OP = "WithConfig";

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
                sterms.stream().map(strategoTerms::fromStratego).collect(ImList.Immutable.toImmutableList());
        final Optional<? extends ITerm> result = call(env, term, terms);
        return result.map(strategoTerms::toStratego);
    }

    protected abstract Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException;

    ///////////////////////////////////////
    // Helper methods for checking specs //
    ///////////////////////////////////////

    protected void reportOverlappingRules(final Spec spec) {
        final Map<String, Set<Rule>> rulesWithEquivalentPatterns = spec.rules().getAllEquivalentRules();
        if(!rulesWithEquivalentPatterns.isEmpty()) {
            logger.error("+--------------------------------------+");
            logger.error("| FOUND RULES WITH EQUIVALENT PATTERNS |");
            logger.error("+--------------------------------------+");
            for(Map.Entry<String, Set<Rule>> entry : rulesWithEquivalentPatterns.entrySet()) {
                logger.error("| Overlapping rules for: {}", entry.getKey());
                for(Rule rule : entry.getValue()) {
                    logger.error("| * {}", rule);
                }
            }
            logger.error("+--------------------------------------+");
        }
    }

    protected void reportInvalidDataLabel(SolverResult<?> analysis, ITerm label) {
        if(!analysis.spec().dataLabels().contains(label)) {
            logger.warn("{} is not a valid relation in this specification. Available relations are {}.", label,
                    analysis.spec().dataLabels());
        }
    }

    protected void reportInvalidEdgeLabel(SolverResult<?> analysis, ITerm label) {
        if(!analysis.spec().edgeLabels().contains(label)) {
            logger.warn("{} is not a valid data label in this specification. Available labels are {}.", label,
                    analysis.spec().edgeLabels());
        }
    }

    protected IDebugContext getDebugContext(ITerm levelTerm) throws InterpreterException {
        final String levelString =
                M.stringValue().match(levelTerm).orElseThrow(() -> new InterpreterException("Expected log level."));
        final @Nullable Level level = levelString.equalsIgnoreCase("None") ? null : Level.parse(levelString);
        final IDebugContext debug = level != null ? new LoggerDebugContext(getLogger(), level) : new NullDebugContext();
        return debug;
    }

    protected IProgress getProgress(ITerm progressTerm) throws InterpreterException {
        // @formatter:off
        return M.cases(
            M.tuple0(t -> new NullProgress()),
            M.blobValue(IProgress.class)
        ).match(progressTerm).orElseThrow(() -> new InterpreterException("Expected progress."));
        // @formatter:on
    }

    protected ICancel getCancel(ITerm cancelTerm) throws InterpreterException {
        // @formatter:off
        return M.cases(
            M.tuple0(t -> new NullCancel()),
            M.blobValue(ICancel.class)
        ).match(cancelTerm).orElseThrow(() -> new InterpreterException("Expected cancel."));
        // @formatter:on
    }

    protected ILogger getLogger() {
        return logger;
    }

    protected static SolverResult<?> getResult(ITerm current) throws InterpreterException {
        // @formatter:off
        return M.cases(
            M.appl2(WITH_CONFIG_OP, M.term(), M.blobValue(SolverResult.class), (t, c, r) -> r),
            M.blobValue(SolverResult.class)
        ).match(current).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on
    }

    protected static IStatixProjectConfig getConfig(ITerm current) throws InterpreterException {
        return M.appl2(WITH_CONFIG_OP, M.blobValue(IStatixProjectConfig.class), M.term(), (t, c, r) -> c).match(current)
                .orElse(IStatixProjectConfig.NULL);
    }

}
