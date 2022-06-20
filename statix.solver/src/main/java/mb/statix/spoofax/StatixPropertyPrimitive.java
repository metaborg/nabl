package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import mb.nabl2.terms.ITerm;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.ITermProperty.Multiplicity;
import mb.statix.solver.persistent.SolverResult;

public abstract class StatixPropertyPrimitive extends StatixPrimitive {

    protected static final String PROP_OP = "Prop";
    protected static final String PROPERTIES_OP = "Properties";
    protected static final String STX_PROP_OP = "StxProp";

    protected static final ITerm PROP_TYPE = B.newAppl("Type");
    protected static final ITerm PROP_REF = B.newAppl("Ref");

    protected static final ITerm MULT_SINGLETON = B.newAppl("Singleton");
    protected static final ITerm MULT_BAG = B.newAppl("Bag");

    public StatixPropertyPrimitive(String name) {
        super(name);
    }

    public StatixPropertyPrimitive(String name, int tvars) {
        super(name, tvars);
    }

    protected ITerm explicate(Multiplicity multiplicity) {
        return multiplicity == Multiplicity.SINGLETON ? MULT_SINGLETON : MULT_BAG;
    }

    protected ITerm instantiateValue(ITermProperty property, SolverResult analysis) {

        switch(property.multiplicity()) {
            case BAG: {
                return B.newList(Streams.stream(property.values()).map(analysis.state().unifier()::findRecursive)
                        .collect(ImmutableList.toImmutableList()));
            }
            case SINGLETON: {
                return analysis.state().unifier().findRecursive(property.value());
            }
            default:
                throw new IllegalStateException("Unknown multiplicity " + property.multiplicity());
        }
    }

}
