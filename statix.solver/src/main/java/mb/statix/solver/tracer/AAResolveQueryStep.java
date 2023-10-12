package mb.statix.solver.tracer;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.reference.Env;
import mb.scopegraph.oopsla20.terms.newPath.ResolutionPath;
import mb.statix.constraints.IResolveQuery;
import mb.statix.scopegraph.Scope;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Collection;

@Value.Immutable
public abstract class AAResolveQueryStep implements IStep {

    @Value.Parameter @Override public abstract IResolveQuery constraint();

    @Value.Parameter @Override public abstract StepResult result();

    @Value.Parameter public abstract @Nullable Env<Scope, ITerm, ITerm> answer();

    @Override public <R> R match(Cases<R> cases) {
        return cases.caseResolveQuery((AAResolveQueryStep) this);
    }

}
