package org.metaborg.meta.nabl2.spoofax.primitives;

import java.util.List;
import java.util.Optional;

import org.metaborg.meta.nabl2.spoofax.analysis.IScopeGraphContext;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.terms.generic.TB;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple2;
import org.metaborg.meta.nabl2.util.tuples.Tuple2;
import org.spoofax.interpreter.core.InterpreterException;

public class SG_fresh extends ScopeGraphPrimitive {

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
            return TB.newString(context.unit(resourceAndBase._1()).fresh().fresh(resourceAndBase._2()));
        });
        // @formatter:on
    }

}