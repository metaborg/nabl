package org.metaborg.meta.nabl2.solver.components;

import java.util.Optional;

import org.metaborg.meta.nabl2.constraints.ast.IAstConstraint;
import org.metaborg.meta.nabl2.constraints.messages.IMessageInfo;
import org.metaborg.meta.nabl2.solver.ASolver;
import org.metaborg.meta.nabl2.solver.ISolver.SeedResult;
import org.metaborg.meta.nabl2.solver.ISolver.SolveResult;
import org.metaborg.meta.nabl2.solver.SolverCore;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.util.collections.IProperties;

public class AstComponent extends ASolver {

    private final IProperties.Transient<TermIndex, ITerm, ITerm> properties;

    public AstComponent(SolverCore core, IProperties.Transient<TermIndex, ITerm, ITerm> initial) {
        super(core);
        this.properties = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    public SeedResult seed(IProperties.Immutable<TermIndex, ITerm, ITerm> solution, IMessageInfo message)
            throws InterruptedException {
        solution.stream().forEach(entry -> {
            putProperty(entry._1(), entry._2(), entry._3());
        });
        return SeedResult.empty();
    }

    public Optional<SolveResult> solve(IAstConstraint constraint) throws InterruptedException {
        SolveResult result = constraint.match(IAstConstraint.Cases.of(astp -> {
            putProperty(astp.getIndex(), astp.getKey(), astp.getValue());
            return SolveResult.empty();
        }));
        return Optional.of(result);
    }

    private void putProperty(TermIndex index, ITerm key, ITerm value) {
        Optional<ITerm> prev = properties.getValue(index, key);
        if(!prev.isPresent()) {
            properties.putValue(index, key, value);
        } else {
            if(!value.equals(prev)) {
                throw new IllegalStateException("Should not set the same AST property multiple times.");
            }
        }
    }

    public IProperties.Immutable<TermIndex, ITerm, ITerm> finish() {
        return properties.freeze();
    }

}