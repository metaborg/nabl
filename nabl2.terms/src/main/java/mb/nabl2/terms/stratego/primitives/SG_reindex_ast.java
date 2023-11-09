package mb.nabl2.terms.stratego.primitives;

import io.usethesource.capsule.Map;
import mb.nabl2.terms.stratego.StrategoTermIndices;
import mb.nabl2.terms.stratego.TermIndex;
import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.library.ssl.StrategoImmutableMap;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;

/**
 * Assigns unique AST indices to the terms of the given subtree, even if they already have AST indices.
 * The strategy returns an ImmutableMap() from the old indices to the new indices.
 * <p>
 * The first argument is the resource name to use, which can be an empty string.
 * The second argument is the first index to use.
 * The result is a tuple of (term, map).
 */
public final class SG_reindex_ast extends AbstractPrimitive {

    public SG_reindex_ast() {
        super(SG_reindex_ast.class.getSimpleName(), 0, 2);
    }

    @Override public boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars) throws InterpreterException {
        final String resource = TermUtils.toJavaString(tvars[0]);
        final int startIndex = TermUtils.toJavaInt(tvars[1]);
        final ITermFactory factory = env.getFactory();

        // Reindex
        final Tuple2<IStrategoTerm, Map.Immutable<TermIndex, TermIndex>> result = StrategoTermIndices.reindex(env.current(), resource, startIndex, factory);

        // Transform the TermIndex objects to proper Stratego terms
        final Map.Immutable<IStrategoTerm, IStrategoTerm> newMap = CapsuleUtil.mapEntries(result._2(), (k, v) -> {
            final IStrategoTerm key = StrategoTermIndices.build(k, factory);
            final IStrategoTerm value = StrategoTermIndices.build(v, factory);
            return Tuple2.of(key, value);
        });

        // Create a tuple (term, map)
        final IStrategoTerm tuple = factory.makeTuple(
            result._1(),
            new StrategoImmutableMap(newMap).withWrapper(factory)
        );
        env.setCurrent(tuple);
        return true;
    }

}
