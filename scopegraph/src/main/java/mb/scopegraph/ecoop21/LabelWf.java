package mb.scopegraph.ecoop21;

import java.util.Optional;

public interface LabelWf<L> {

    Optional<LabelWf<L>> step(L l);

    boolean accepting();

    @SuppressWarnings("unchecked") static <L> LabelWf<L> any() {
        return (LabelWf<L>) ANY;
    }

    @SuppressWarnings("unchecked") static <L> LabelWf<L> none() {
        return (LabelWf<L>) NONE;
    }

    @SuppressWarnings("rawtypes") static final LabelWf ANY = new LabelWf() {

        @Override public boolean accepting() {
            return true;
        }

        @Override public Optional<LabelWf> step(@SuppressWarnings("unused") Object l) {
            return Optional.of(this);
        }

        @Override public String toString() {
            return ".*";
        }

    };

    @SuppressWarnings("rawtypes") static final LabelWf NONE = new LabelWf() {

        @Override public boolean accepting() {
            return false;
        }

        @Override public Optional<LabelWf> step(@SuppressWarnings("unused") Object l) {
            return Optional.empty();
        }

        @Override public String toString() {
            return "{}";
        }

    };

}