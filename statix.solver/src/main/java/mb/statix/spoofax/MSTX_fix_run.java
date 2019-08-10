package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.ITerm;
import mb.statix.modular.util.TTimings;

public class MSTX_fix_run extends StatixPrimitive {

    @Inject public MSTX_fix_run() {
        super(MSTX_fix_run.class.getSimpleName(), 1);
    }

    @Override
    protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms) throws InterpreterException {
        int val = M.integerValue().match(terms.get(0)).get();
        if (val == 0) {
            TTimings.unfixRun();
        } else if (val == 1) {
            TTimings.fixRun();
        } else {
            return Optional.empty();
        }
        
        return Optional.of(B.newInt(0));
    }
}