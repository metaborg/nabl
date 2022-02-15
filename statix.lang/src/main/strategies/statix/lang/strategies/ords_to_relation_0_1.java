package statix.lang.strategies;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import mb.nabl2.terms.stratego.StrategoBlob;
import mb.scopegraph.oopsla20.reference.EdgeOrData;
import mb.scopegraph.oopsla20.reference.RelationLabelOrder;
import mb.scopegraph.relations.IRelation;
import mb.scopegraph.relations.RelationDescription;
import mb.scopegraph.relations.RelationException;
import mb.scopegraph.relations.impl.Relation;

public class ords_to_relation_0_1 extends Strategy {

    public static final Strategy instance = new ords_to_relation_0_1();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm labelPairs, IStrategoTerm relation) {
        final IRelation.Transient<EdgeOrData<IStrategoTerm>> order =
                Relation.Transient.of(RelationDescription.STRICT_PARTIAL_ORDER);
        if(!TermUtils.isList(labelPairs)) {
            throw new IllegalArgumentException("Expected list with label pairs, got " + labelPairs);
        }
        for(IStrategoTerm labelPair : labelPairs) {
            final Tuple2<IStrategoTerm, IStrategoTerm> lbls = Labels.parseLabelPair(labelPair);
            final IStrategoTerm left = Labels.normalizeLabel(lbls._1(), relation);
            final IStrategoTerm right = Labels.normalizeLabel(lbls._2(), relation);
            try {
                order.add(EdgeOrData.edge(left), EdgeOrData.edge(right));
            } catch(RelationException e) {
                throw new IllegalArgumentException("Relation order not strictly partial.", e);
            }
        }

        return new StrategoBlob(new RelationLabelOrder<>(order.freeze()));
    }

}
