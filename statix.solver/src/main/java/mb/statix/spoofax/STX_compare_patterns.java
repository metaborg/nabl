package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.Pattern;

public class STX_compare_patterns extends StatixPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_compare_patterns() {
        super(STX_compare_patterns.class.getSimpleName(), 0);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final Tuple2<Pattern, Pattern> ps = M.tuple2(StatixTerms.pattern(), StatixTerms.pattern(), (t, p1, p2) -> {
            return Tuple2.of(p1, p2);
        }).match(term).orElseThrow(() -> new InterpreterException("Expected tuple of patterns, got " + term + "."));
        return Pattern.leftRightOrdering.compare(ps._1(), ps._2()).map(B::newInt);
    }

}