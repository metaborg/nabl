package mb.nabl2.spoofax.primitives;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;

import mb.nabl2.stratego.ConstraintTerms;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.stratego.StrategoTerms;

public abstract class NaBL2Primitive extends AbstractPrimitive {

    final protected int tvars;

    public NaBL2Primitive(String name) {
        this(name, 0);
    }

    public NaBL2Primitive(String name, int tvars) {
        super(name, 0, tvars);
        this.tvars = tvars;
    }

    @Override public final boolean call(IContext env, Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        final List<IStrategoTerm> termArgs = Arrays.asList(tvars);
        return call(env.current(), termArgs, env.getFactory()).map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    private Optional<? extends IStrategoTerm> call(IStrategoTerm sterm, List<IStrategoTerm> sterms,
            ITermFactory factory) throws InterpreterException {
        if(sterms.size() != tvars) {
            throw new InterpreterException("Expected " + tvars + " term arguments, but got " + sterms.size());
        }
        final StrategoTerms strategoTerms = new StrategoTerms(factory);
        final ITerm term = ConstraintTerms.specialize(strategoTerms.fromStratego(sterm));
        final List<ITerm> terms = sterms.stream().map(strategoTerms::fromStratego).map(ConstraintTerms::specialize)
                .collect(Collectors.toList());
        final Optional<? extends ITerm> resultTerm = call(term, terms);
        return resultTerm.map(ConstraintTerms::explicate).map(strategoTerms::toStratego);
    }

    protected abstract Optional<? extends ITerm> call(ITerm term, List<ITerm> terms) throws InterpreterException;

}