package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;

import com.google.inject.Inject;

import mb.nabl2.terms.IListTerm;
import mb.nabl2.terms.ITerm;

public class STX_analyze extends StatixPrimitive {

    private static final ILogger logger = LoggerUtils.logger(STX_analyze.class);

    @Inject public STX_analyze() {
        super(STX_analyze.class.getSimpleName(), 3);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm ast, List<ITerm> terms)
            throws InterpreterException {
        final ITerm specTerm = terms.get(0);
        final ITerm extTerm = terms.get(1);
        final String init = M.stringValue().match(terms.get(2))
                .orElseThrow(() -> new InterpreterException("Expected init/1 name."));

        final IListTerm errors = B.EMPTY_LIST;
        final IListTerm warnings = B.EMPTY_LIST;
        final IListTerm notes = B.EMPTY_LIST;
        final ITerm result = B.newTuple(ast, errors, warnings, notes);
        return Optional.of(result);
    }

}