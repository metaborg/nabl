package mb.nabl2.spoofax.primitives;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Optional;

import org.spoofax.interpreter.core.InterpreterException;

import mb.nabl2.spoofax.analysis.IScopeGraphContext;
import mb.nabl2.terms.ITerm;
import mb.nabl2.util.ImmutableTuple2;
import mb.nabl2.util.Tuple2;

public class SG_fresh extends ScopeGraphContextPrimitive {

    public SG_fresh() {
        super(SG_fresh.class.getSimpleName(), 0, 0);
    }

    @Override public Optional<ITerm> call(IScopeGraphContext<?> context, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        // @formatter:off
        return M.<Tuple2<String,String>>cases(
            M.string(s -> ImmutableTuple2.of(s.getValue(), "")),
            M.tuple2(M.stringValue(), M.stringValue(), (t, s1, s2) -> ImmutableTuple2.of(s1,s2))
        ).match(term).map(resourceAndBase -> {
            return B.newString(context.unit(resourceAndBase._1()).fresh().fresh(resourceAndBase._2()));
        });
        // @formatter:on
    }

}