package meta.flowspec.java.solver;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.pcollections.HashPMap;
import org.pcollections.IntTreePMap;
import org.pcollections.PMap;

import meta.flowspec.java.ImmutablePair;
import meta.flowspec.java.Pair;
import meta.flowspec.java.lattice.CompleteLattice;

public abstract class MaximalFixedPoint {
    public static <Label, Property> Pair<PMap<Label, Property>, PMap<Label, Property>> intraProcedural(
            CompleteLattice<Property> lattice, BiFunction<Label, Property, Property> transfer, ControlFlow<Label> flow,
            Property extremalValue) {

        Set<Label> workList = flow.lhsSet();
        final Set<Label> allLabels = flow.rhsSet().plusAll(flow.lhsSet());
        final Map<Label, Property> analysis = allLabels.stream().map(l -> ImmutablePair.of(l, lattice.bottom()))
                .collect(Collectors.toMap(ImmutablePair::left, ImmutablePair::right));

        for (Label label : flow.inits) {
            analysis.put(label, extremalValue);
        }

        while (!workList.isEmpty()) {
            final Label from = workList.iterator().next();
            workList.remove(from);
            for (Label to : flow.getRhsSet(from)) {
                final Property transferred = transfer.apply(from, analysis.get(from));
                if (!(lattice.lte(transferred, analysis.get(to)))) {
                    final Property newToValue = lattice.lub(transferred, analysis.get(to));
                    analysis.put(to, newToValue);
                    workList.add(to);
                }
            }
        }

        PMap<Label, Property> pre = HashPMap.empty(IntTreePMap.empty());
        pre = pre.plusAll(analysis);
        PMap<Label, Property> post = HashPMap.empty(IntTreePMap.empty());
        for (Map.Entry<Label, Property> e : analysis.entrySet()) {
            e.setValue(transfer.apply(e.getKey(), e.getValue()));
        }
        post = post.plusAll(analysis);
        return ImmutablePair.of(pre, post);
    }
}
