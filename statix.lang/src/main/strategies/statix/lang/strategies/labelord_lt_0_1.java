package statix.lang.strategies;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.ResolutionException;

public class labelord_lt_0_1 extends Strategy {

    public static final Strategy instance = new labelord_lt_0_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm labelPairTerm, IStrategoTerm orderTerm) {
        final LabelOrder<IStrategoTerm> order = Labels.readLabelOrderBlob(orderTerm);
        final Tuple2<IStrategoTerm, IStrategoTerm> labelPair = Labels.parseLabelPair(labelPairTerm);

        try {
            return order.lt(EdgeOrData.edge(labelPair._1()), EdgeOrData.edge(labelPair._2())) ? labelPairTerm : null;
        } catch(ResolutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
