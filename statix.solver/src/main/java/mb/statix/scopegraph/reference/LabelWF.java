package mb.statix.scopegraph.reference;

import java.io.Serializable;
import java.util.Optional;

public interface LabelWF<L> {

    Optional<LabelWF<L>> step(L l) throws ResolutionException, InterruptedException;
    
    /**
     * @param l
     *      the label
     * 
     * @return
     *      true if this labelWf can make a step with the given label, false otherwise
     * 
     * @throws ResolutionException
     *      If resolution failed or is delayed.
     * @throws InterruptedException
     *      If stepping is interrupted.
     */
    boolean canStep(L l) throws ResolutionException, InterruptedException;

    boolean accepting() throws ResolutionException, InterruptedException;

    /**
     * NOTE: The returned LabelWF is serializable
     * 
     * @return
     *      a label wf that matches any path
     */
    static <L> LabelWF<L> ANY() {
        class SerializableLabelWF implements LabelWF<L>, Serializable {
            private static final long serialVersionUID = 1L;

            @Override public Optional<LabelWF<L>> step(L l) {
                return Optional.of(this);
            }
            
            @Override public boolean canStep(L l) {
                return true;
            }

            @Override public boolean accepting() {
                return true;
            }
        };
        
        return new SerializableLabelWF();
    }

}