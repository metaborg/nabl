package meta.flowspec.java.solver;

import org.pcollections.PSet;

import meta.flowspec.java.pcollections.MapSetPRelation;
import meta.flowspec.java.pcollections.PRelation;

public class ControlFlow<Label> extends MapSetPRelation<Label, Label> {
    public final PSet<Label> inits;
    public final PSet<Label> finals;
    
    public ControlFlow(PSet<Label> inits, PSet<Label> finals, PRelation<Label, Label> flow) {
        super(flow);
        this.inits = inits;
        this.finals = finals;
    }
}
