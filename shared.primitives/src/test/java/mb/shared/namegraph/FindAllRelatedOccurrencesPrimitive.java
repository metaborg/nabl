package mb.shared.namegraph;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;

public class FindAllRelatedOccurrencesPrimitive extends AbstractPrimitive {

    private final static String NAME = "FindAllRelatedOccurrences";

    public FindAllRelatedOccurrencesPrimitive() {
        super(NAME, 0, 0);
    }

    @Override
    public boolean call(IContext env, org.spoofax.interpreter.stratego.Strategy[] svars, IStrategoTerm[] tvars)
            throws InterpreterException {
        return call(env.current(), env.getFactory()).map(t -> {
            env.setCurrent(t);
            return true;
        }).orElse(false);
    }

    private Optional<? extends IStrategoTerm> call(IStrategoTerm current, ITermFactory factory)
            throws InterpreterException {
        NameIndex selectedOccurrence = new NameIndex(current.getSubterm(0));
        List<IStrategoTerm> resolutionRelation = TermUtils.toJavaList(current.getSubterm(1));
        NameGraph nameGraph = new NameGraph(resolutionRelation);

        Set<NameIndex> cluster = nameGraph.find(selectedOccurrence).get();
        IStrategoList targetIndices = factory
                .makeList(cluster.stream().map(term -> term.getTermIndex()).collect(Collectors.toList()));

        return Optional.of(targetIndices);

    }

}
