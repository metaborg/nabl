package mb.statix.spoofax;

import static mb.nabl2.terms.build.TermBuild.B;
import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import org.metaborg.util.collection.ImList;
import org.metaborg.util.tuple.Tuple2;
import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.InterpreterException;


import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.terms.stratego.TermIndex;
import mb.statix.solver.ITermProperty;
import mb.statix.solver.persistent.SolverResult;

public class STX_get_all_properties extends StatixPropertyPrimitive {


    @jakarta.inject.Inject @javax.inject.Inject public STX_get_all_properties() {
        super(STX_get_all_properties.class.getSimpleName(), 1);
    }

    @Override protected Optional<? extends ITerm> call(IContext env, ITerm term, List<ITerm> terms)
            throws InterpreterException {
        final SolverResult<?> analysis = M.blobValue(SolverResult.class).match(terms.get(0))
                .orElseThrow(() -> new InterpreterException("Expected solver result."));

        final SortedMap<TermIndex, SortedMap<ITerm, ITermProperty>> groupedProps =
                new TreeMap<>(TermIndexComparator.instance);

        for(Map.Entry<Tuple2<TermIndex, ITerm>, ITermProperty> prop : analysis.state().termProperties().entrySet()) {
            final TermIndex index = prop.getKey()._1();
            final ITerm propName = prop.getKey()._2();
            final ITermProperty propValue = prop.getValue();

            final SortedMap<ITerm, ITermProperty> group =
                    groupedProps.computeIfAbsent(index, __ -> new TreeMap<>(PropComparator.instance));

            group.put(propName, propValue);
        }

        final ImList.Mutable<ITerm> propSets = ImList.Mutable.of();
        for(Map.Entry<TermIndex, SortedMap<ITerm, ITermProperty>> rawSet : groupedProps.entrySet()) {
            final ImList.Mutable<ITerm> props = ImList.Mutable.of();
            final TermIndex index = rawSet.getKey();

            for(Map.Entry<ITerm, ITermProperty> rawProp : rawSet.getValue().entrySet()) {
                final ITerm name = rawProp.getKey();
                final ITerm value = instantiateValue(rawProp.getValue(), analysis);
                final ITerm multiplicity = explicate(rawProp.getValue().multiplicity());

                props.add(B.newAppl(STX_PROP_OP, name, value, multiplicity));
            }
            propSets.add(B.newTuple(index, B.newList(props.freeze())));
        }

        return Optional.of(B.newAppl(PROPERTIES_OP, B.newList(propSets.freeze())));

    }

    private static class TermIndexComparator implements Comparator<TermIndex> {

        public static final TermIndexComparator instance = new TermIndexComparator();

        private TermIndexComparator() {
        }

        @Override public int compare(TermIndex index1, TermIndex index2) {
            int resource = index1.getResource().compareTo(index2.getResource());
            return resource != 0 ? resource : Integer.compare(index1.getId(), index2.getId());
        }

    }

    private static class PropComparator implements Comparator<ITerm> {

        public static final PropComparator instance = new PropComparator();

        private static final IMatcher<String> propNames = M.appl1(PROP_OP, M.stringValue(), (appl, val) -> val);

        private PropComparator() {
        }

        @Override public int compare(ITerm prop1, ITerm prop2) {
            boolean firstType = prop1.equals(PROP_TYPE);
            boolean secondType = prop2.equals(PROP_TYPE);

            if(firstType) {
                return secondType ? 0 : -1;
            } else if(secondType) {
                return 1;
            }

            boolean firstRef = prop1.equals(PROP_REF);
            boolean secondRef = prop2.equals(PROP_REF);

            if(firstRef) {
                return secondRef ? 0 : -1;
            } else if(secondRef) {
                return 1;
            }

            final String name1 = getPropName(prop1);
            final String name2 = getPropName(prop2);

            return name1.compareTo(name2);
        }

        private String getPropName(ITerm prop) {
            return propNames.match(prop).orElseThrow(() -> new IllegalStateException("Expected prop, got " + prop));
        }

    }

}
