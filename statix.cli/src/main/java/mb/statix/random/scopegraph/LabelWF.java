package mb.statix.random.scopegraph;

import java.util.Optional;

import mb.statix.scopegraph.reference.ResolutionException;

public interface LabelWF<L> {

    Optional<LabelWF<L>> step(L l) throws ResolutionException, InterruptedException;

    boolean accepting() throws ResolutionException, InterruptedException;

    static <L> LabelWF<L> ANY() {
        return new LabelWF<L>() {

            @Override public Optional<LabelWF<L>> step(L l) {
                return Optional.of(this);
            }

            @Override public boolean accepting() {
                return true;
            }

        };
    }

}