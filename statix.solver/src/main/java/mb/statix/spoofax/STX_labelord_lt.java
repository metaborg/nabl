package mb.statix.spoofax;


import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;

import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.LabelOrder;
import mb.scopegraph.oopsla20.reference.ResolutionException;

public class STX_labelord_lt extends AbstractPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_labelord_lt() {
        super(STX_labelord_lt.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext context, Strategy[] svars, IStrategoTerm[] tvars) {
        final IStrategoTerm labelPairTerm = context.current();
        final IStrategoTerm orderTerm = tvars[0];

        final LabelOrder<IStrategoTerm> order = LabelUtils.readLabelOrderBlob(orderTerm);
        final Tuple2<IStrategoTerm, IStrategoTerm> labelPair = LabelUtils.parseLabelPair(labelPairTerm);

        try {
            return order.lt(EdgeOrData.edge(labelPair._1()), EdgeOrData.edge(labelPair._2()));
        } catch(ResolutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
