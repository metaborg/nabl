package mb.statix.concurrent.p_raffrayi.nameresolution;

import java.util.Optional;

public interface LabelWf<L> {

    Optional<LabelWf<L>> step(L l);

    boolean accepting();

    static <L> LabelWf<L> any() {
        return new LabelWf<L>() {

            @Override public boolean accepting() {
                return true;
            }

            @Override public Optional<LabelWf<L>> step(L l) {
                return Optional.of(this);
            }

        };
    }

}