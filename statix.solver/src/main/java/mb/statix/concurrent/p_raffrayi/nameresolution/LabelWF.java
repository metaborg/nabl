package mb.statix.concurrent.p_raffrayi.nameresolution;

import java.util.Optional;

public interface LabelWF<L> {

    Optional<LabelWF<L>> step(L l);

    boolean accepting();

}