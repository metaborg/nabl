package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;
import org.metaborg.util.log.LoggerUtils;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.terms.stratego.TermOrigin;
import mb.nabl2.terms.substitution.ISubstitution;
import mb.nabl2.terms.substitution.PersistentSubstitution;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
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
        final ListMultimap<String, Rule> rulesWithEquivalentPatterns = spec.rules().getAllEquivalentRules();
        if(!rulesWithEquivalentPatterns.isEmpty()) {
            logger.error("+--------------------------------------+");
            logger.error("| FOUND RULES WITH EQUIVALENT PATTERNS |");
            logger.error("+--------------------------------------+");
            for(Map.Entry<String, Collection<Rule>> entry : rulesWithEquivalentPatterns.asMap().entrySet()) {
                logger.error("| Overlapping rules for: {}", entry.getKey());
                for(Rule rule : entry.getValue()) {
                    logger.error("| * {}", rule);
                }
            }
            logger.error("+--------------------------------------+");
        }
    }

    protected void reportInvalidDataLabel(SolverResult analysis, ITerm label) {
        if(!analysis.spec().dataLabels().contains(label)) {
            logger.warn("{} is not a valid relation in this specification. Available relations are {}.", label,
                    analysis.spec().dataLabels());
        }
    }

    protected void reportInvalidEdgeLabel(SolverResult analysis, ITerm label) {
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

    ////////////////////////////////////////////////
    // Helper methods for creating error messages //
    ////////////////////////////////////////////////

    protected void addMessage(final IMessage message, final IConstraint constraint, final IUniDisunifier unifier,
            IStatixProjectConfig config, final Collection<ITerm> errors, final Collection<ITerm> warnings,
            final Collection<ITerm> notes) {
        Tuple2<Iterable<String>, ITerm> message_origin = formatMessage(message, constraint, unifier, config);

        final String messageText = Streams.stream(message_origin._1()).filter(s -> !s.isEmpty())
                .map(s -> cleanupString(s)).collect(Collectors.joining("<br>\n&gt;&nbsp;", "", "<br>\n"));

        final ITerm messageTerm = B.newTuple(message_origin._2(), B.newString(messageText));
        switch(message.kind()) {
            case ERROR:
                errors.add(messageTerm);
                break;
            case WARNING:
                warnings.add(messageTerm);
                break;
            case NOTE:
                notes.add(messageTerm);
                break;
            case IGNORE:
                break;
        }

    }

    public static Tuple2<Iterable<String>, ITerm> formatMessage(final IMessage message, final IConstraint constraint,
            final IUniDisunifier unifier, IStatixProjectConfig config) {
        final TermFormatter formatter = Solver.shallowTermFormatter(unifier,
                config.messageTermDepth(config.messageTermDepth(IStatixProjectConfig.DEFAULT_MESSAGE_TERM_DEPTH)));
        final int maxTraceLength =
                config.messageTraceLength(config.messageTraceLength(IStatixProjectConfig.DEFAULT_MESSAGE_TRACE_LENGTH));

        ITerm originTerm = message.origin().flatMap(t -> getOriginTerm(t, unifier)).orElse(null);
        final Deque<String> trace = Lists.newLinkedList();
        IConstraint current = constraint;
        int traceCount = 0;
        while(current != null) {
            if(originTerm == null) {
                originTerm = findOriginArgument(current, unifier).orElse(null);
            }
            if(maxTraceLength < 0 || ++traceCount <= maxTraceLength) {
                trace.addLast(current.toString(formatter));
            }
            current = current.cause().orElse(null);
        }
        if(maxTraceLength > 0 && traceCount > maxTraceLength) {
            trace.addLast("... trace truncated ...");
        }

        // add constraint message
        trace.addFirst(message.toString(formatter, () -> constraint.toString(formatter), completeness -> {
            final ISubstitution.Transient subst = PersistentSubstitution.Transient.of();
            completeness.vars().forEach(var -> {
                ITerm sub = unifier.findRecursive(var);
                if(!sub.equals(var)) {
                    subst.put(var, sub);
                }
            });
            return completeness.apply(subst.freeze()).entrySet().stream().flatMap(e -> {
                String scope = e.getKey().toString();
                return e.getValue().elementSet().stream().map(edge -> scope + "-" + edge.toString());
            }).collect(Collectors.joining(", "));
        }));

        // use empty origin if none was found
        if(originTerm == null) {
            originTerm = B.newTuple();
        }

        return Tuple2.of(trace, originTerm);
    }

    private static Optional<ITerm> findOriginArgument(IConstraint constraint, IUniDisunifier unifier) {
        // @formatter:off
        final Function1<IConstraint, Stream<ITerm>> terms = Constraints.cases(
            onArith -> Stream.empty(),
            onConj -> Stream.empty(),
            onEqual -> Stream.empty(),
            onExists -> Stream.empty(),
            onFalse -> Stream.empty(),
            onInequal -> Stream.empty(),
            onNew -> Stream.empty(),
            onResolveQuery -> Stream.empty(),
            onTellEdge -> Stream.empty(),
            onTermId -> Stream.empty(),
            onTermProperty -> Stream.empty(),
            onTrue -> Stream.empty(),
            onTry -> Stream.empty(),
            onUser -> onUser.args().stream()
        );
        return terms.apply(constraint)
                .flatMap(t -> Streams.stream(getOriginTerm(t, unifier)))
                .findFirst();
        // @formatter:on
    }

    private static Optional<ITerm> getOriginTerm(ITerm term, IUniDisunifier unifier) {
        // @formatter:off
        return Optional.of(unifier.findTerm(term))
            .filter(t -> TermIndex.get(t).isPresent())
            .filter(t -> TermOrigin.get(t).isPresent()) // HACK Ignore terms without origin, such as empty lists
            .map(t -> B.newTuple(ImmutableList.of(), t.getAttachments()));
        // @formatter:on
    }

    private static String cleanupString(String string) {
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r\n", "<br>")
                .replace("\n", "<br>").replace("\r", "<br>").replace("\t", "&Tab;");
    }

    protected static SolverResult getResult(ITerm current) throws InterpreterException {
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
