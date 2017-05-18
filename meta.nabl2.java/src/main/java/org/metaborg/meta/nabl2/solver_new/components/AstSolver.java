package org.metaborg.meta.nabl2.solver_new.components;

import java.util.Optional;

import org.immutables.serial.Serial;
import org.immutables.value.Value;
import org.metaborg.meta.nabl2.constraints.ast.CAstProperty;
import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.messages.ImmutableMessageInfo;
import org.metaborg.meta.nabl2.constraints.messages.MessageContent;
import org.metaborg.meta.nabl2.constraints.messages.MessageKind;
import org.metaborg.meta.nabl2.solver.IProperties;
import org.metaborg.meta.nabl2.solver.Properties;
import org.metaborg.meta.nabl2.solver_new.ASolver;
import org.metaborg.meta.nabl2.solver_new.SolverCore;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;

public class AstSolver extends ASolver<IAstConstraint, AstSolver.AstResult> {

    private final IProperties.Mutable<TermIndex> properties;

    public AstSolver(SolverCore core) {
        super(core);
        this.properties = Properties.Mutable.of();
    }

    // ------------------------------------------------------------------------------------------------------//

    @Override public boolean add(IAstConstraint constraint) throws InterruptedException {
        solve(constraint);
        work();
        return true;
    }

    @Override public AstResult finish() {
        return ImmutableAstResult.of(properties.freeze());
    }

    // ------------------------------------------------------------------------------------------------------//

    private boolean solve(IAstConstraint constraint) {
        return constraint.match(IAstConstraint.Cases.of(this::solve));
    }

    private boolean solve(CAstProperty constraint) {
        Optional<ITerm> prev = properties.putValue(constraint.getIndex(), constraint.getKey(), constraint.getValue());
        if(prev.isPresent()) {
            unify(prev.get(), constraint.getValue(), ImmutableMessageInfo.of(MessageKind.ERROR, MessageContent.of(),
                    constraint.getMessageInfo().getOriginTerm()));
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------------//

    @Value.Immutable
    @Serial.Version(42L)
    public static abstract class AstResult {

        @Value.Parameter public abstract IProperties.Immutable<TermIndex> properties();

    }

}