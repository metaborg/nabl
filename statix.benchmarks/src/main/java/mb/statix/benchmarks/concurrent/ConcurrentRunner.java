package mb.statix.benchmarks.concurrent;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.ThreadCancel;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.statix.concurrent.p_raffrayi.IBroker;
import mb.statix.concurrent.p_raffrayi.IBrokerResult;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.solver.ProjectTypeChecker;
import mb.statix.constraints.CUser;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;

public class ConcurrentRunner {

    public static java.util.Map<String, Rule> makeFileUnits(java.util.Map<String, ITerm> files,
            String projectConstraint, String fileConstraint) {
        ITermVar s = B.newVar("", "s");
        java.util.Map<String, Rule> units = new HashMap<>();
        units.put(".", Rule.of("", ImmutableList.of(P.newVar(s)), new CUser(projectConstraint, ImmutableList.of(s))));
        for(Entry<String, ITerm> file : files.entrySet()) {
            units.put(file.getKey(), Rule.of("", ImmutableList.of(P.newVar(s)),
                    new CUser(fileConstraint, ImmutableList.of(s, file.getValue()))));
        }
        return units;
    }

    public static IBrokerResult<Scope, ITerm, ITerm, SolverResult> run(Spec spec, java.util.Map<String, Rule> units)
            throws ExecutionException, InterruptedException {

        final ICancel cancel = new ThreadCancel();
        final IDebugContext debug = new NullDebugContext();

        final IScopeImpl<Scope> scopeImpl = new ScopeImpl();

        final IBroker<Scope, ITerm, ITerm, SolverResult> broker = new Broker<>(scopeImpl, spec.allLabels(), cancel);
        broker.add("<ROOT>", new ProjectTypeChecker(units, spec, debug));

        broker.run();

        return broker.result().get();
    }

}