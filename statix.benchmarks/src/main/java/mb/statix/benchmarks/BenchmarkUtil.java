package mb.statix.benchmarks;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermPattern.P;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;

import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;
import org.metaborg.util.task.NullCancel;
import org.metaborg.util.task.NullProgress;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.TAFTermReader;
import org.spoofax.terms.util.TermUtils;

import com.google.common.collect.ImmutableList;

import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.StrategoTerms;
import mb.nabl2.util.Tuple2;
import mb.statix.concurrent.p_raffrayi.IBroker;
import mb.statix.concurrent.p_raffrayi.IBrokerResult;
import mb.statix.concurrent.p_raffrayi.IScopeImpl;
import mb.statix.concurrent.p_raffrayi.impl.Broker;
import mb.statix.concurrent.p_raffrayi.impl.ScopeImpl;
import mb.statix.concurrent.solver.ProjectTypeChecker;
import mb.statix.constraints.CUser;
import mb.statix.constraints.Constraints;
import mb.statix.scopegraph.terms.Scope;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.log.IDebugContext;
import mb.statix.solver.log.NullDebugContext;
import mb.statix.solver.persistent.Solver;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.Rule;
import mb.statix.spec.Spec;
import mb.statix.spoofax.StatixTerms;

public class BenchmarkUtil {

    public static Spec loadSpecFromATerm(String resource) throws IOException {
        ITermFactory termFactory = new TermFactory();
        TAFTermReader reader = new TAFTermReader(termFactory);
        final InputStream is = BenchmarkUtil.class.getResourceAsStream(resource);
        IStrategoTerm sterm = reader.parseFromStream(is);
        StrategoTerms converter = new StrategoTerms(termFactory);
        ITerm term = converter.fromStratego(sterm);
        Spec spec = StatixTerms.spec().match(term).orElseThrow(() -> new IllegalArgumentException("Expected spec."));
        return spec;
    }

    public static java.util.Map<String, ITerm> loadFilesFromATerm(String resource) throws IOException {
        ITermFactory termFactory = new TermFactory();
        TAFTermReader reader = new TAFTermReader(termFactory);
        final InputStream is = BenchmarkUtil.class.getResourceAsStream(resource);
        IStrategoTerm filesSTerm = reader.parseFromStream(is);
        StrategoTerms converter = new StrategoTerms(termFactory);
        IStrategoList filesSTerms = TermUtils.toList(filesSTerm);
        java.util.Map<String, ITerm> files = new HashMap<>();
        for(IStrategoTerm fileSTerm : filesSTerms) {
            fileSTerm = TermUtils.toTuple(fileSTerm);
            String fileName = TermUtils.toJavaStringAt(fileSTerm, 0);
            IStrategoTerm astSTerm = fileSTerm.getSubterm(1);
            astSTerm = StrategoTermIndices.index(astSTerm, fileName, termFactory);
            ITerm ast = converter.fromStratego(astSTerm);
            files.put(fileName, ast);
        }
        return files;
    }

    public static Tuple2<IState.Immutable, java.util.Map<String, IConstraint>> makeFileConstraints(
            IState.Immutable state, java.util.Map<String, ITerm> files, String projectConstraint,
            String fileConstraint) {
        IState.Transient _state = state.melt();
        Scope root = _state.freshScope("s");
        java.util.Map<String, IConstraint> units = new HashMap<>();
        units.put(".", new CUser(projectConstraint, ImmutableList.of(root)));
        for(Entry<String, ITerm> file : files.entrySet()) {
            units.put(file.getKey(), new CUser(fileConstraint, ImmutableList.of(root, file.getValue())));
        }
        return Tuple2.of(_state.freeze(), units);
    }

    public static SolverResult runTraditional(Spec spec, IState.Immutable state,
            java.util.Map<String, IConstraint> units) throws Exception, InterruptedException {

        final ICancel cancel = new NullCancel();
        final IProgress progress = new NullProgress();
        final IDebugContext debug = new NullDebugContext();

        return Solver.solve(spec, state, Constraints.conjoin(units.values()), debug, cancel, progress);
    }

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

    public static IBrokerResult<Scope, ITerm, ITerm, SolverResult> runConcurrent(Spec spec,
            java.util.Map<String, Rule> units, int parallelism) throws Exception, InterruptedException {

        final ICancel cancel = new NullCancel();
        final IDebugContext debug = new NullDebugContext();

        final IScopeImpl<Scope> scopeImpl = new ScopeImpl();

        final IBroker<Scope, ITerm, ITerm, SolverResult> broker =
                new Broker<>(scopeImpl, spec.allLabels(), cancel, parallelism);
        broker.add("<ROOT>", new ProjectTypeChecker(units, spec, debug));

        broker.run();

        return broker.result().get();
    }

}