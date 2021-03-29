package mb.statix.concurrent.p_raffrayi.nameresolution;

import java.util.Optional;

public interface LabelWf<L> {

    Optional<LabelWf<L>> step(L l);

    boolean accepting();

    static <L> LabelWf<L> any() {
        return ANY;
    }

    static final LabelWf ANY = new LabelWf() {

        @Override public boolean accepting() {
            return true;
        }

        @Override public Optional<LabelWf> step(Object l) {
            return Optional.of(this);
        }

        @Override public String toString() {
            return ".*";
        }

    };

}