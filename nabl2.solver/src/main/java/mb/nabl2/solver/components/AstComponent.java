package mb.nabl2.solver.components;

import java.util.Optional;

import mb.nabl2.constraints.IConstraint;
import mb.nabl2.constraints.ast.CAstProperty;
import mb.nabl2.constraints.ast.IAstConstraint;
import mb.nabl2.constraints.equality.CEqual;
import mb.nabl2.constraints.messages.IMessageInfo;
import mb.nabl2.log.Logger;
import mb.nabl2.solver.ASolver;
import mb.nabl2.solver.SeedResult;
import mb.nabl2.solver.SolveResult;
import mb.nabl2.solver.SolverCore;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.nabl2.util.collections.IProperties;

public class AstComponent extends ASolver {

    private static final Logger log = Logger.logger(AstComponent.class);

    private final IProperties.Transient<TermIndex, ITerm, ITerm> properties;

    public AstComponent(SolverCore core, IProperties.Transient<TermIndex, ITerm, ITerm> initial) {
        super(core);
        this.properties = initial;
    }

    // ------------------------------------------------------------------------------------------------------//

    public SeedResult seed(IProperties.Immutable<TermIndex, ITerm, ITerm> solution, IMessageInfo message)
            throws InterruptedException {
        solution.stream().forEach(entry -> {
            putProperty(entry._1(), entry._2(), entry._3(), message);
        });
        return SeedResult.empty();
    }

    public SolveResult solve(IAstConstraint constraint) {
        return constraint.match(IAstConstraint.Cases.of(astp -> solve(astp)));
    }

    private SolveResult solve(CAstProperty astp) {
        return putProperty(astp.getIndex(), astp.getKey(), astp.getValue(), astp.getMessageInfo())
                .map(cc -> SolveResult.constraints(cc)).orElseGet(() -> SolveResult.empty());
    }

    private Optional<IConstraint> putProperty(TermIndex index, ITerm key, ITerm value, IMessageInfo message) {
        Optional<ITerm> prev = properties.getValue(index, key);
        if(!prev.isPresent()) {
            log.debug("new prop: {}@{} |-> {}", key, index, value);
            properties.putValue(index, key, value);
            return Optional.empty();
        } else {
            log.debug("eq prop: {}@{}: {} == {}", key, index, value, prev.get());
            return Optional.of(CEqual.of(value, prev.get(), message));
        }
    }

    public IProperties.Immutable<TermIndex, ITerm, ITerm> finish() {
        return properties.freeze();
    }

}
