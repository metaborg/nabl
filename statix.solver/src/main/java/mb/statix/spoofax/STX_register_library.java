package mb.statix.spoofax;

import static mb.nabl2.terms.matching.TermMatch.M;
import static mb.nabl2.terms.build.TermBuild.B;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.terms.ITerm;
import mb.scopegraph.oopsla20.IScopeGraph;
import mb.statix.library.IStatixLibrary;
import mb.statix.scopegraph.Scope;
import mb.statix.solver.IState;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.solver.persistent.State;

public class STX_register_library extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_register_library() {
        super(STX_register_library.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Tuple2<String, IStatixLibrary> name_lib =
                M.tuple2(M.stringValue(), IStatixLibrary.matcher(), (appl, name, lib) -> Tuple2.of(name, lib))
                        .match(term).orElseThrow(
                                () -> new InterpreterException("Expected pair of string and scope graph library."));

        final String libName = name_lib._1();
        final IStatixLibrary lib = name_lib._2();

        final Scope globalScope =
                Scope.matcher().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected scope."));

        final SolverResult<?> initial = M.blobValue(SolverResult.class).match(terms.get(1))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final Tuple2<? extends Set<Scope>, IScopeGraph.Immutable<Scope, ITerm, ITerm>> initLib =
                lib.initialize(Collections.singletonList(globalScope), name -> Scope.of(libName, name));

        final IState.Transient state = initial.state().melt();
        state.add(State.of().withScopeGraph(initLib._2()).with__scopes(CapsuleUtil.toSet(initLib._1())));

        return Optional.of(B.newBlob(initial.withState(state.freeze())));
    }

}
