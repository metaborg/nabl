package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.TermSimplifier;
import mb.nabl2.terms.ITerm;

public class SG_focus_term extends NaBL2Primitive {

    public SG_focus_term() {
        super(SG_focus_term.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(ITerm term, List<ITerm> terms) throws InterpreterException {
        final String resource = M.stringValue().match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected resource as first term argument."));
        return Optional.of(TermSimplifier.focus(resource, term));
    }

}