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
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.TermFormatter;
import mb.statix.constraints.Constraints;
import mb.statix.constraints.messages.IMessage;
import mb.statix.solver.IConstraint;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.LoggerDebugContext;
import mb.statix.solver.persistent.Solver;
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

    protected IDebugContext getDebugContext(ITerm levelTerm) throws InterpreterException {
        final String levelString =
                M.stringValue().match(levelTerm).orElseThrow(() -> new InterpreterException("Expected log level."));
        final Level level = levelString.equalsIgnoreCase("None") ? Level.Debug : Level.parse(levelString);
        final IDebugContext debug = new LoggerDebugContext(logger, level);
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

    ////////////////////////////////////////////////
    // Helper methods for creating error messages //
    ////////////////////////////////////////////////

    protected void addMessage(final IMessage message, final IConstraint constraint, final IUniDisunifier unifier,
            IStatixProjectConfig config, final Collection<ITerm> errors, final Collection<ITerm> warnings,
            final Collection<ITerm> notes) {
        Tuple2<Iterable<String>, ITerm> message_origin = formatMessage(message, constraint, unifier, config);

        final String messageText = Streams.stream(message_origin._1()).filter(s -> !s.isEmpty())
                .map(s -> cleanupString(s)).collect(Collectors.joining("<br>\n&gt;&nbsp;"));

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
        trace.addFirst(message.toString(formatter, () -> constraint.toString(formatter)));

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
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
