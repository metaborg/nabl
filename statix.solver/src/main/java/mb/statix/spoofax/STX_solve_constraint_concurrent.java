package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.collection.ImList;
import org.metaborg.util.future.IFuture;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;


import io.usethesource.capsule.Map;
import mb.nabl2.terms.ITerm;
import mb.p_raffrayi.IUnitResult;
import mb.p_raffrayi.PRaffrayiSettings;
import mb.p_raffrayi.impl.Broker;
import mb.p_raffrayi.impl.Result;
import mb.statix.concurrent.IStatixProject;
import mb.statix.concurrent.ProjectResult;
import mb.statix.concurrent.ProjectTypeChecker;
import mb.statix.concurrent.SolverState;
import mb.statix.concurrent.StatixProject;
import mb.statix.concurrent.nameresolution.ScopeImpl;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.Message;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.constraints.messages.TextPart;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;
import mb.statix.solver.tracer.EmptyTracer;
import mb.statix.solver.tracer.EmptyTracer.Empty;
import mb.statix.spec.Rule;
import mb.statix.spec.RuleName;
import mb.statix.spec.Spec;

public class STX_solve_constraint_concurrent extends StatixConstraintPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_solve_constraint_concurrent() {
        super(STX_solve_constraint_concurrent.class.getSimpleName());
    }

    @Override protected SolverResult<Empty> solve(Spec spec, IConstraint constraint, IDebugContext debug, IProgress progress,
            ICancel cancel) throws InterruptedException, ExecutionException {

        final IStatixProject<Empty> project = StatixProject.<Empty>builder().resource("").changed(true)
                .rule(Rule.of("resolve", RuleName.empty(), ImList.Immutable.of(P.newWld()), constraint)).build();
        final IFuture<IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<Empty>, SolverState>>> future =
                Broker.run("", PRaffrayiSettings.of(false, false, false, false),
                        new ProjectTypeChecker<>(project, spec, debug, EmptyTracer::new), new ScopeImpl(), spec.allLabels(), cancel, progress);
        final IUnitResult<Scope, ITerm, ITerm, Result<Scope, ITerm, ITerm, ProjectResult<Empty>, SolverState>> result = future.asJavaCompletion().get();
        if(!result.allFailures().isEmpty() || result.result().analysis().exception() != null) {
            final SolverResult.Builder<Empty> resultBuilder =
                    SolverResult.<Empty>builder().spec(spec).state(State.of()).traceResult(Empty.of())
                            .completeness(Completeness.Immutable.of());

            final Map.Transient<IConstraint, IMessage> messages = CapsuleUtil.transientMap();
            if(result.result().analysis().exception() != null) {
                logger.error("Failure solving constraint.", result.result().analysis().exception());
                messages.__put(constraint, toMessage(result.result().analysis().exception()));
            }
            for(Throwable ex : result.allFailures()) {
                logger.error("Failure solving constraint.", ex);
            }

            resultBuilder.messages(messages.freeze());
            return resultBuilder.build();
        } else {
            final SolverResult<Empty> resultConfig = result.result().analysis().solveResult();
            final IState.Immutable state = resultConfig.state().withScopeGraph(result.scopeGraph());
            return resultConfig.withState(state);
        }
    }

    private IMessage toMessage(Throwable ex) {
        return new Message(MessageKind.ERROR,
                Arrays.asList(new TextPart("Failure during constraint solving: " + ex.getMessage())), null);
    }

}
