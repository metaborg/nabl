package mb.statix.spoofax;


import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.stratego.Strategy;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import mb.nabl2.terms.stratego.StrategoBlob;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.RelationLabelOrder;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.RelationException;
import mb.scopegraph.relations.impl.Relation;

public class STX_ords_to_relation extends AbstractPrimitive {

    @jakarta.inject.Inject @javax.inject.Inject public STX_ords_to_relation() {
        super(STX_ords_to_relation.class.getSimpleName(), 0, 1);
    }

    @Override public boolean call(IContext context, Strategy[] svars, IStrategoTerm[] tvars) {
        final IStrategoTerm labelPairs = context.current();
        final IStrategoTerm relation = tvars[0];

        final IRelation.Transient<EdgeOrData<IStrategoTerm>> order =
                Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        if(!TermUtils.isList(labelPairs)) {
            throw new IllegalArgumentException("Expected list with label pairs, got " + labelPairs);
        }
        for(IStrategoTerm labelPair : labelPairs) {
            final Tuple2<IStrategoTerm, IStrategoTerm> lbls = LabelUtils.parseLabelPair(labelPair);
            final IStrategoTerm left = LabelUtils.normalizeLabel(lbls._1(), relation);
            final IStrategoTerm right = LabelUtils.normalizeLabel(lbls._2(), relation);
            try {
                order.add(EdgeOrData.edge(left), EdgeOrData.edge(right));
            } catch(RelationException e) {
                throw new IllegalArgumentException("Relation order not strictly partial.", e);
            }
        }

        final IStrategoTerm result = new StrategoBlob(new RelationLabelOrder<>(order.freeze()));
        context.setCurrent(result);
        return true;
    }

}
