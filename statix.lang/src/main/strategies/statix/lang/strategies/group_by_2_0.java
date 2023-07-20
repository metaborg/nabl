package statix.lang.strategies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

public class group_by_2_0 extends Strategy {

    public static final Strategy instance = new group_by_2_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm current, Strategy getKey, Strategy mapValue) {
        final ITermFactory TF = context.getFactory();
        if(!TermUtils.isList(current)) {
            throw new java.lang.IllegalArgumentException("Expected list, got " + current);
        }
        final Map<IStrategoTerm, Set<IStrategoTerm>> groups = new HashMap<>();
        for(IStrategoTerm item : current.getAllSubterms()) {
            final IStrategoTerm key = getKey.invoke(context, item);
            if(key == null) {
                continue;
            }
            final IStrategoTerm value = mapValue.invoke(context, item);
            groups.computeIfAbsent(key, k -> new HashSet<>()).add(value);
        }
        final List<IStrategoTerm> groupTerms = new ArrayList<>();
        for(Map.Entry<IStrategoTerm, Set<IStrategoTerm>> entry : groups.entrySet()) {
            groupTerms.add(TF.makeTuple(entry.getKey(), TF.makeList(entry.getValue())));
        }
        return TF.makeList(groupTerms);
    }

}