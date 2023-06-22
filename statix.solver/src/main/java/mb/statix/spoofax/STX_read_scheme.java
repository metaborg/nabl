package mb.statix.spoofax;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.terms.ITerm;

public class STX_read_scheme extends StatixPrimitive {

    @Inject public STX_read_scheme() {
        super(STX_read_scheme.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        return StatixTerms.schema().match(term).map(__ -> term);
    }

}
