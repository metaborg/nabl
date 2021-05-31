package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.solver.persistent.SolverResult;

public class STX_extract_messages extends StatixPrimitive {

    private static final String WITH_CONFIG_OP = "WithConfig";

    @Inject public STX_extract_messages() {
        super(STX_extract_messages.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {

        final IStatixProjectConfig config = getConfig(term);
        final SolverResult result = getResult(term);
        final IUniDisunifier unifier = result.state().unifier();

        final List<ITerm> errorList = Lists.newArrayList();
        final List<ITerm> warningList = Lists.newArrayList();
        final List<ITerm> noteList = Lists.newArrayList();
        result.messages().forEach((c, m) -> addMessage(m, c, unifier, config, errorList, warningList, noteList));

        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.newList(warningList);
        final IListTerm notes = B.newList(noteList);
        final ITerm resultTerm = B.newTuple(errors, warnings, notes);
        return Optional.of(resultTerm);
    }

    private static SolverResult getResult(ITerm current) throws InterpreterException {
        // @formatter:off
        return M.cases(
            M.appl2(WITH_CONFIG_OP, M.term(), M.blobValue(SolverResult.class), (t, c, r) -> r),
            M.blobValue(SolverResult.class)
        ).match(current).orElseThrow(() -> new InterpreterException("Expected solver result."));
        // @formatter:on
    }

    private static IStatixProjectConfig getConfig(ITerm current) throws InterpreterException {
        return M.appl2(WITH_CONFIG_OP, M.blobValue(IStatixProjectConfig.class), M.term(), (t, c, r) -> c).match(current)
                .orElse(IStatixProjectConfig.NULL);
    }


}