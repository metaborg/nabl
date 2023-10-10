package mb.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.util.Ref;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.PrintlineLogger;

import io.usethesource.capsule.Set;
import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.equality.CInequal;
import mb.nabl2.constraints.equality.IEqualityConstraint;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.constraints.messages.MessageContent;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SeedResult;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.solver.exceptions.VariableDelayException;
import mb.nabl2.solver.messages.IMessages;
import mb.nabl2.solver.messages.Messages;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.OccursException;
import mb.nabl2.terms.unification.u.IUnifier;
import mb.nabl2.unification.UnificationMessages;

public class EqualityComponent extends ASolver {

    private static final PrintlineLogger log = PrintlineLogger.logger(EqualityComponent.class);

    private final Ref<IUnifier.Immutable> unifier;

    public EqualityComponent(SolverCore core, Ref<IUnifier.Immutable> unifier) {
        super(core);
        this.unifier = unifier;
    }

    public SeedResult seed(IUnifier.Immutable solution, IMessageInfo message) throws InterruptedException {
        final Set.Transient<IConstraint> constraints = CapsuleUtil.transientSet();
        final IMessages.Transient messages = Messages.Transient.of();
        try {
            final IUnifier.Transient unifier = this.unifier.get().melt();
            if(!unifier.unify(solution).isPresent()) {
                log.debug("seeding failed: {}", message);
                messages.add(message.withContent(MessageContent.of("Unification failed.")));
            } else {
                this.unifier.set(unifier.freeze());
            }
        } catch(OccursException e) {
            log.debug("seeding failed: {}", message);
            final MessageContent content = MessageContent.of("Recursive unifier");
            messages.add(message.withContent(content));
        }
        return SeedResult.builder().constraints(constraints.freeze()).messages(messages.freeze()).build();
    }

    public SolveResult solve(IEqualityConstraint constraint) throws VariableDelayException {
        return constraint.matchOrThrow(IEqualityConstraint.CheckedCases.of(this::solve, this::solve));
    }

    public IUnifier.Immutable finish() {
        return unifier.get();
    }

    // ------------------------------------------------------------------------------------------------------//

    private SolveResult solve(CEqual constraint) {
        final ITerm left = constraint.getLeft();
        final ITerm right = constraint.getRight();
        final IUnifier.Transient unifier = this.unifier.get().melt();
        IUnifier.Immutable unifyResult = null;
        try {
            unifyResult = unifier.unify(left, right).orElse(null);
        } catch(OccursException ex) {
        }
        if(unifyResult != null) {
            final SolveResult solveResult = SolveResult.builder().unifierDiff(unifyResult).build();
            this.unifier.set(unifier.freeze());
            log.debug("unification succeeded {}", unifyResult);
            return solveResult;
        } else {
            final MessageContent content = UnificationMessages.getError(left, right);
            final IMessageInfo message = (constraint.getMessageInfo().withDefaultContent(content));
            final SolveResult solveResult = SolveResult.messages(message);
            log.debug("unification failed {}", content.toString(t -> this.unifier.get().findRecursive(t).toString()));
            return solveResult;
        }
    }

    private SolveResult solve(CInequal constraint) throws VariableDelayException {
        final ITerm left = constraint.getLeft();
        final ITerm right = constraint.getRight();
        Optional<? extends IUnifier.Immutable> result = unifier().diff(left, right);
        if(!result.isPresent()) {
            log.debug("dis-unification succeeded");
            return SolveResult.empty();
        } else if(result.get().isEmpty()) {
            MessageContent content = MessageContent.builder().append(constraint.getLeft().toString()).append(" and ")
                    .append(constraint.getRight().toString()).append(" must be inequal, but are not.").build();
            IMessageInfo message = constraint.getMessageInfo().withDefaultContent(content);
            log.debug("dis-unification failed {} == {}", constraint.getLeft(), constraint.getRight());
            return SolveResult.messages(message);
        } else {
            final Iterable<ITermVar> termVars = Iterables2.fromConcat(unifier().getVars(left), unifier().getVars(right));
            log.debug("dis-unification delayed {}", termVars);
            throw new VariableDelayException(termVars);
        }
    }

    // ------------------------------------------------------------------------------------------------------//

}
