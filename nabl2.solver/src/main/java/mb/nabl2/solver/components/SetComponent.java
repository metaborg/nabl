package mb.nabl2.solver.components;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.functions.Function1;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.unit.Unit;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.Set.Immutable;
import io.usethesource.capsule.SetMultimap;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.constraints.messages.MessageInfo;
import mb.nabl2.constraints.sets.CDistinct;
import mb.nabl2.constraints.sets.CEvalSet;
import mb.nabl2.constraints.sets.CSubsetEq;
import mb.nabl2.constraints.sets.ISetConstraint;
import mb.nabl2.log.Logger;
import mb.nabl2.sets.IElement;
import mb.nabl2.sets.ISetProducer;
import mb.nabl2.sets.SetEvaluator;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.exceptions.CriticalEdgeDelayException;
import mb.nabl2.solver.exceptions.DelayException;
import mb.nabl2.solver.exceptions.InterruptedDelayException;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import static mb.nabl2.terms.matching.Transform.T;
import mb.scopegraph.pepm16.CriticalEdgeException;
import mb.scopegraph.pepm16.StuckException;

public class SetComponent extends ASolver {

    private static final Logger log = Logger.logger(SetComponent.class);

    private static final String NAME_OP = "NAME";

    private final IMatcher<ISetProducer<ITerm>> evaluator;

    public SetComponent(SolverCore core, IMatcher<ISetProducer<ITerm>> elems) {
        super(core);
        this.evaluator = SetEvaluator.matcher(elems);
    }

    public SolveResult solve(ISetConstraint constraint) throws DelayException {
        return constraint.matchOrThrow(ISetConstraint.CheckedCases.of(this::solve, this::solve, this::solve));
    }

    public Unit finish() {
        return Unit.unit;
    }

    // ------------------------------------------------------------------------------------------------------//

    private SolveResult solve(CSubsetEq constraint) throws DelayException {
        ITerm left = constraint.getLeft();
        ITerm right = constraint.getRight();
        if(!unifier().isGround(left) && unifier().isGround(right)) {
            Iterable<ITermVar> setVars = Iterables2.fromConcat(unifier().getVars(left), unifier().getVars(right));
            log.debug("* delaying {}", setVars);
            throw new VariableDelayException(setVars);
        }
        Optional<ISetProducer<ITerm>> maybeLeftSet = evaluator.match(left, unifier());
        Optional<ISetProducer<ITerm>> maybeRightSet = evaluator.match(right, unifier());
        if(!(maybeLeftSet.isPresent() && maybeRightSet.isPresent())) {
            log.debug("* not a set {}");
            return SolveResult.empty(); // FIXME: error message when an argument is not a set?
        }
        final Set.Immutable<IElement<ITerm>> leftSet;
        final Set.Immutable<IElement<ITerm>> rightSet;
        try {
            leftSet = maybeLeftSet.get().apply();
            rightSet = maybeRightSet.get().apply();
        } catch(CriticalEdgeException e) {
            log.debug("* delaying - critical edge", e);
            throw new CriticalEdgeDelayException(e);
        } catch(StuckException e) {
            IMessageInfo message = constraint.getMessageInfo()
                    .withDefaultContent(MessageContent.builder().append("Name set is stuck.").build());
            log.debug("* failing - stuck", e);
            return SolveResult.messages(message);
        } catch(InterruptedException e) {
            throw new InterruptedDelayException(e);
        }
        SetMultimap.Immutable<Object, IElement<ITerm>> leftProj = SetEvaluator.project(leftSet, constraint.getProjection());
        SetMultimap.Immutable<Object, IElement<ITerm>> rightProj = SetEvaluator.project(rightSet, constraint.getProjection());
        SetMultimap.Transient<Object, IElement<ITerm>> result = SetMultimap.Transient.of();
        for(Object key : leftProj.keySet()) {
            if(!rightProj.containsKey(key)) {
                result.__insert(key, leftProj.get(key));
            }
        }
        if(result.isEmpty()) {
            log.debug("* succeed");
            return SolveResult.empty();
        } else {
            MessageContent content =
                    MessageContent.builder().append(B.newAppl(NAME_OP)).append(" not in ").append(right).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), result.freeze().values());
            log.debug("* fail");
            return SolveResult.messages(messages);
        }
    }

    private SolveResult solve(CDistinct constraint) throws DelayException {
        ITerm setTerm = constraint.getSet();
        if(!unifier().isGround(setTerm)) {
            final Immutable<ITermVar> setVars = unifier().getVars(setTerm);
            log.debug("* delaying {}", setVars);
            throw new VariableDelayException(setVars);
        }
        Optional<ISetProducer<ITerm>> maybeSet = evaluator.match(setTerm, unifier());
        if(!(maybeSet.isPresent())) {
            log.debug("* not a set");
            return SolveResult.empty();
        }
        Set<IElement<ITerm>> set;
        try {
            set = maybeSet.get().apply();
        } catch(CriticalEdgeException e) {
            log.debug("* delaying - critical edge", e);
            throw new CriticalEdgeDelayException(e);
        } catch(StuckException e) {
            log.debug("* failing - stuck", e);
            IMessageInfo message = constraint.getMessageInfo()
                    .withDefaultContent(MessageContent.builder().append("Name set is stuck.").build());
            return SolveResult.messages(message);
        } catch(InterruptedException e) {
            throw new InterruptedDelayException(e);
        }
        SetMultimap.Immutable<Object, IElement<ITerm>> proj = SetEvaluator.project(set, constraint.getProjection());
        List<IElement<ITerm>> duplicates = new ArrayList<>();
        for(Object key : proj.keySet()) {
            Collection<IElement<ITerm>> values = proj.get(key);
            if(values.size() > 1) {
                duplicates.addAll(values);
            }
        }
        if(duplicates.isEmpty()) {
            log.debug("* succeed");
            return SolveResult.empty();
        } else {
            MessageContent content = MessageContent.builder().append(B.newAppl(NAME_OP)).append(" has duplicates in ")
                    .append(setTerm).build();
            Iterable<IMessageInfo> messages =
                    makeMessages(constraint.getMessageInfo().withDefaultContent(content), duplicates);
            log.debug("* fail");
            return SolveResult.messages(messages);
        }
    }

    private SolveResult solve(CEvalSet constraint) throws DelayException {
        ITerm setTerm = constraint.getSet();
        if(!unifier().isGround(setTerm)) {
            Immutable<ITermVar> setVars = unifier().getVars(setTerm);
            log.debug("* delaying {}", setVars);
            throw new VariableDelayException(setVars);
        }
        Optional<ISetProducer<ITerm>> maybeSet = evaluator.match(setTerm, unifier());
        if(!(maybeSet.isPresent())) {
            log.debug("* not a set");
            return SolveResult.empty();
        }
        Set<IElement<ITerm>> set;
        try {
            set = maybeSet.get().apply();
        } catch(CriticalEdgeException e) {
            log.debug("* delaying - critical edge", e);
            throw new CriticalEdgeDelayException(e);
        } catch(StuckException e) {
            log.debug("* failing - stuck", e);
            IMessageInfo message = constraint.getMessageInfo()
                    .withDefaultContent(MessageContent.builder().append("Name set is stuck.").build());
            return SolveResult.messages(message);
        } catch(InterruptedException e) {
            throw new InterruptedDelayException(e);
        }
        List<ITerm> elements = set.stream().map(i -> i.getValue()).collect(Collectors.toList());
        return SolveResult
                .constraints(CEqual.of(constraint.getResult(), B.newList(elements), constraint.getMessageInfo()));

    }

    private Iterable<IMessageInfo> makeMessages(IMessageInfo template, Collection<IElement<ITerm>> elements) {
        boolean nameOrigin = M.appl0(NAME_OP).match(template.getOriginTerm(), unifier()).isPresent();
        if(nameOrigin && !elements.isEmpty()) {
            return elements.stream().<IMessageInfo>map(e -> {
                Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> e.getName()).match(t, unifier()));
                return MessageInfo.of(template.getKind(), template.getContent().apply(f), e.getPosition());
            }).collect(Collectors.toList());
        } else {
            ITerm es = B.newList(elements.stream().map(e -> e.getName()).collect(Collectors.toList()));
            Function1<ITerm, ITerm> f = T.sometd(t -> M.appl0(NAME_OP, a -> es).match(t, unifier()));
            return Iterables2.singleton(
                    MessageInfo.of(template.getKind(), template.getContent().apply(f), template.getOriginTerm()));
        }
    }

}
