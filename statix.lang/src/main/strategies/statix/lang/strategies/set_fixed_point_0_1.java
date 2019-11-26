package statix.lang.strategies;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;

import io.usethesource.capsule.Set;
import mb.nabl2.util.CapsuleUtil;

public class set_fixed_point_0_1 extends Strategy {

    private static final ILogger log = LoggerUtils.logger(set_fixed_point_0_1.class);

    static final Strategy instance = new set_fixed_point_0_1();

    @Override public IStrategoTerm invoke(final Context context, final IStrategoTerm current,
            final IStrategoTerm base) {
        final ITermFactory TF = context.getFactory();

        final Set.Immutable<IStrategoTerm> baseSet = termToSet(base);

        if(!Tools.isTermList(current)) {
            throw new java.lang.IllegalArgumentException("Expected list of equations, got " + current);
        }
        final HashMap<IStrategoTerm, Eq> eqs = Maps.newHashMap();
        final Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values = Maps.newHashMap();
        for(IStrategoTerm varEq : current.getAllSubterms()) {
            if(!Tools.isTermTuple(varEq) || varEq.getSubtermCount() != 2) {
                throw new java.lang.IllegalArgumentException(
                        "Expected triple of variable, init, and components, got " + varEq);
            }
            final IStrategoTerm var = varEq.getSubterm(0);
            final Eq eq = parse(varEq.getSubterm(1));
            eqs.put(var, eq);
            values.put(var, baseSet);
        }

        boolean again = true;
        while(again) {
            again = false;
            for(Map.Entry<IStrategoTerm, Eq> varEq : eqs.entrySet()) {
                final Set.Immutable<IStrategoTerm> varResult = varEq.getValue().apply(values);
                final Set.Immutable<IStrategoTerm> prevResult = values.put(varEq.getKey(), varResult);
                again |= prevResult == null || !prevResult.equals(varResult);
            }
        }

        final List<IStrategoTerm> resultTerms = Lists.newArrayList();
        values.forEach((var, set) -> {
            resultTerms.add(TF.makeTuple(var, TF.makeList(set)));
        });
        return TF.makeList(resultTerms);
    }

    private Eq parse(IStrategoTerm term) {
        if(Tools.isTermList(term)) {
            return new Const(CapsuleUtil.toSet(Arrays.asList(term.getAllSubterms())));
        }
        if(!Tools.isTermAppl(term)) {
            throw new IllegalArgumentException("Expected equation, got " + term);
        }
        if(Tools.hasConstructor((IStrategoAppl) term, "Union", 1)) {
            final IStrategoTerm components = term.getSubterm(0);
            if(!Tools.isTermList(components) || components.getSubtermCount() == 0) {
                throw new IllegalArgumentException("Expected equations, got " + components);
            }
            return new Union(Streams.stream(components).map(t -> parse(t)).collect(ImmutableList.toImmutableList()));
        } else if(Tools.hasConstructor((IStrategoAppl) term, "Intersection", 1)) {
            final IStrategoTerm components = term.getSubterm(0);
            if(!Tools.isTermList(components) || components.getSubtermCount() == 0) {
                throw new IllegalArgumentException("Expected equations, got " + components);
            }
            return new Intersection(
                    Streams.stream(components).map(t -> parse(t)).collect(ImmutableList.toImmutableList()));
        } else {
            return new Var(term);
        }
    }

    private Set.Immutable<IStrategoTerm> termToSet(IStrategoTerm term) {
        if(!Tools.isTermList(term)) {
            throw new java.lang.IllegalArgumentException("Expected base set, got " + term);
        }
        return CapsuleUtil.toSet(Arrays.asList(term.getAllSubterms()));
    }


    private abstract class Eq {

        public abstract Set.Immutable<IStrategoTerm> apply(Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values);

    }

    private class Union extends Eq {

        private final List<Eq> components;

        public Union(Iterable<Eq> components) {
            this.components = ImmutableList.copyOf(components);
        }

        @Override public Set.Immutable<IStrategoTerm> apply(Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values) {
            return components.stream().map(c -> c.apply(values)).reduce((s1, s2) -> Set.Immutable.union(s1, s2)).get();
        }

        @Override public String toString() {
            return "union(" + components.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
        }


    }

    private class Intersection extends Eq {

        private final List<Eq> components;

        public Intersection(Iterable<Eq> components) {
            this.components = ImmutableList.copyOf(components);
        }

        @Override public Set.Immutable<IStrategoTerm> apply(Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values) {
            return components.stream().map(c -> c.apply(values)).reduce((s1, s2) -> Set.Immutable.intersect(s1, s2))
                    .get();
        }

        @Override public String toString() {
            return "isect(" + components.stream().map(Object::toString).collect(Collectors.joining(", ")) + ")";
        }

    }

    private class Var extends Eq {

        private IStrategoTerm var;

        public Var(IStrategoTerm var) {
            this.var = var;
        }

        @Override public Set.Immutable<IStrategoTerm> apply(Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values) {
            return values.getOrDefault(var, Set.Immutable.of());
        }

        @Override public String toString() {
            return var.toString();
        }

    }

    private class Const extends Eq {

        private Set.Immutable<IStrategoTerm> value;

        public Const(Set.Immutable<IStrategoTerm> value) {
            this.value = value;
        }

        @Override public Set.Immutable<IStrategoTerm> apply(Map<IStrategoTerm, Set.Immutable<IStrategoTerm>> values) {
            return value;
        }

        @Override public String toString() {
            return value.toString();
        }

    }

}