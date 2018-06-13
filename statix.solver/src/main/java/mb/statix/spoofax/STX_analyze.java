package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.statix.solver.Completeness;
import mb.statix.solver.Config;
import mb.statix.solver.DebugContext;
import mb.statix.solver.IConstraint;
import mb.statix.solver.Solver;
import mb.statix.solver.State;
import mb.statix.solver.constraint.CUser;
import mb.statix.spec.Spec;

public class STX_analyze extends StatixPrimitive {
    private static final ILogger logger = LoggerUtils.logger(STX_analyze.class);

    @Inject public STX_analyze() {
        super(STX_analyze.class.getSimpleName(), 2);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm ast, List<ITerm> terms)
            throws InterpreterException {

        final Spec spec =
                StatixTerms.spec().match(terms.get(0)).orElseThrow(() -> new InterpreterException("Expected spec."));

        final String init = M.stringValue().match(terms.get(1))
                .orElseThrow(() -> new InterpreterException("Expected init/1 name."));

        final IConstraint constraint = new CUser(init, Iterables2.singleton(ast));
        final State state = State.of(spec);
        final Config config = Config.of(state, Iterables2.singleton(constraint), new Completeness());
        Config resultConfig;
        try {
            resultConfig = Solver.solve(config, new DebugContext(logger));
        } catch(InterruptedException e) {
            throw new InterpreterException(e);
        }
        final State resultState = resultConfig.state();

        final List<ITerm> errorList = Lists.newArrayList();
        if(resultState.isErroneous()) {
            errorList.add(B.newTuple(ast, B.newString("Has errors.")));
        }
        final Collection<IConstraint> unsolved = resultConfig.constraints();
        if(!unsolved.isEmpty()) {
            logger.warn("Unsolved constraints: {}",
                    unsolved.stream().map(c -> c.toString(resultState.unifier())).collect(Collectors.toList()));
            errorList.add(B.newTuple(ast, B.newString(unsolved.size() + " unsolved constraint(s).")));
        }

        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm resultTerm = B.newTuple(ast, errors, warnings, notes);
        return Optional.of(resultTerm);
    }

}