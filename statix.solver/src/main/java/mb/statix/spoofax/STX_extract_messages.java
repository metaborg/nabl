package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.statix.constraints.messages.MessageUtil;
import mb.statix.solver.persistent.SolverResult;

public class STX_extract_messages extends StatixPrimitive {

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
        result.messages().forEach((c, m) -> MessageUtil.addMessage(m, c, unifier, config, errorList, warningList, noteList));

        final IListTerm errors = B.newList(errorList);
        final IListTerm warnings = B.newList(warningList);
        final IListTerm notes = B.newList(noteList);
        final ITerm resultTerm = B.newTuple(errors, warnings, notes);
        return Optional.of(resultTerm);
    }


}
