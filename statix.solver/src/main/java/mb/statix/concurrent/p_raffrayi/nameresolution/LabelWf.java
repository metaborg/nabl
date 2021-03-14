package mb.statix.concurrent.p_raffrayi.nameresolution;

import java.util.Optional;

public interface LabelWf<L> {

    Optional<LabelWf<L>> step(L l);

    boolean accepting();

}