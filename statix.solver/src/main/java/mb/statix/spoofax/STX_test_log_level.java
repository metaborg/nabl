package mb.statix.spoofax;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.terms.ITerm;
import static mb.nabl2.terms.build.TermBuild.B;

import static mb.nabl2.terms.matching.TermMatch.M;

public class STX_test_log_level extends StatixPrimitive {

    public STX_test_log_level() {
        super(STX_test_log_level.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(org.spoofax.interpreter.core.IContext env, ITerm term,
            List<ITerm> terms) throws InterpreterException {
        final IStatixProjectConfig config = M.blobValue(IStatixProjectConfig.class).match(term)
                .orElseThrow(() -> new IllegalStateException("Expected Statix Project config, but was " + term));

        return Optional.of(B.newString(config.testLogLevel("None")));
    }

}