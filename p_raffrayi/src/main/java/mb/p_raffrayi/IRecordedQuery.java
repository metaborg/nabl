package mb.p_raffrayi;

import mb.p_raffrayi.nameresolution.DataLeq;
import mb.p_raffrayi.nameresolution.DataWf;
import mb.scopegraph.ecoop21.LabelOrder;
import mb.scopegraph.ecoop21.LabelWf;
import mb.scopegraph.oopsla20.reference.Env;

public interface IRecordedQuery<S, L, D> {

    S scope();

    LabelWf<L> labelWf();

    DataWf<S, L, D> dataWf();

    LabelOrder<L> labelOrder();

    DataLeq<S, L, D> dataLeq();

    Env<S, L, D> result();

}
