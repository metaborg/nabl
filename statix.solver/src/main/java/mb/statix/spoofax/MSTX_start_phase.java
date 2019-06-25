package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.taico.util.TTimings;

public class MSTX_start_phase extends StatixPrimitive {

    @Inject public MSTX_start_phase() {
        super(MSTX_start_phase.class.getSimpleName(), 1);
    }

    @Override
    protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        TTimings.startPhase(M.stringValue().match(terms.get(0)).get());
        return Optional.of(B.newInt(0));
    }
}