package mb.statix.solver.persistent.step;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.constraints.CAstId;
import mb.statix.scopegraph.Scope;
import org.immutables.value.Value;

import jakarta.annotation.Nullable;
import java.util.Optional;

@Value.Immutable
public abstract class ACAstIdStep implements IStep {

    @Value.Parameter @Override public abstract CAstId constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable ITerm index();

    @Value.Lazy public Optional<TermIndex> indexAsTermIndex() {
        return Optional.ofNullable(index()).flatMap(TermIndex.matcher()::match);
    }

    @Value.Lazy public Optional<Scope> indexAsScope() {
        return Optional.ofNullable(index()).flatMap(Scope.matcher()::match);
    }

    @Override
    public <R> R match(Cases<R> cases) {
        return cases.caseAstId((CAstIdStep) this);
    }
}
