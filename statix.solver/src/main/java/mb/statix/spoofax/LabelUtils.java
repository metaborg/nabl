package mb.statix.spoofax;

import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.util.TermUtils;

import mb.nabl2.terms.stratego.StrategoBlob;
import mb.scopegraph.oopsla20.reference.LabelOrder;

final class LabelUtils {

    private static final String EOP_OP = "EOP";

    private static final String LABEL_PAIR_OP = "LabelPair";

    private LabelUtils() {

    }

    // EOP

    static boolean isEOP(IStrategoTerm lbl) {
        return TermUtils.isAppl(lbl, EOP_OP, 0);
    }

    static IStrategoTerm normalizeLabel(IStrategoTerm lbl, IStrategoTerm relation) {
        // Use relation for `$` in order.
        return isEOP(lbl) ? relation : lbl;
    }

    // Label pairs

    static Tuple2<IStrategoTerm, IStrategoTerm> parseLabelPair(IStrategoTerm labelPair) {
        if(!TermUtils.isAppl(labelPair, LABEL_PAIR_OP, 2) && !TermUtils.isTuple(labelPair, 2)) {
            throw new java.lang.IllegalArgumentException("Expected label pair, got " + labelPair);
        }

        return Tuple2.of(labelPair.getSubterm(0), labelPair.getSubterm(1));
    }

    static LabelOrder<IStrategoTerm> readLabelOrderBlob(IStrategoTerm blob) {
        if(!(blob instanceof StrategoBlob)) {
            throw new IllegalArgumentException("Expecter label order blob, got " + blob);
        }

        final Object orderValue = ((StrategoBlob) blob).value();
        if(!(orderValue instanceof LabelOrder)) {
            throw new IllegalArgumentException("Expecter label order as blob argument, got " + orderValue);

        }

        @SuppressWarnings("unchecked") final LabelOrder<IStrategoTerm> labelOrder =
                (LabelOrder<IStrategoTerm>) orderValue;
        return labelOrder;
    }

}
