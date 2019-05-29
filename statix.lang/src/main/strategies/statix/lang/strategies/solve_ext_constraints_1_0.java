package statix.lang.strategies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.lang.Context;
import org.strategoxt.lang.Strategy;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import mb.nabl2.relations.IRelation;
import mb.nabl2.relations.ImmutableRelationDescription;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationDescription.Reflexivity;
import mb.nabl2.relations.RelationDescription.Symmetry;
import mb.nabl2.relations.RelationDescription.Transitivity;
import mb.nabl2.relations.RelationException;
import mb.nabl2.relations.impl.Relation;

public class solve_ext_constraints_1_0 extends Strategy {

    static final Strategy instance = new solve_ext_constraints_1_0();

    @Override public IStrategoTerm invoke(Context context, IStrategoTerm constraintsTerm, Strategy combine) {
        final ITermFactory TF = context.getFactory();
        if(!Tools.isTermList(constraintsTerm)) {
            throw new IllegalArgumentException("Expected constraint list, got " + constraintsTerm.toString(1));
        }
        final List<IStrategoTerm> constraintTerms = ImmutableList.copyOf(constraintsTerm.getAllSubterms());
        final Set<IStrategoTerm> vars = new HashSet<>();
        final RelationDescription rd =
                ImmutableRelationDescription.of(Reflexivity.REFLEXIVE, Symmetry.NON_SYMMETRIC, Transitivity.TRANSITIVE);
        final IRelation.Transient<IStrategoTerm> closure = Relation.Transient.of(rd);
        final Multimap<IStrategoTerm, IStrategoList> bounds = HashMultimap.create();
        for(IStrategoTerm constraintTerm : constraintTerms) {
            if(!Tools.isTermTuple(constraintTerm) || constraintTerm.getSubtermCount() != 2) {
                throw new IllegalArgumentException("Expected constraint tuple, got " + constraintTerm.toString(1));
            }
            final IStrategoTerm ext1 = Tools.termAt(constraintTerm, 0);
            final IStrategoTerm ext2 = Tools.termAt(constraintTerm, 1);
            if(Tools.isTermList(ext1)) {
                throw new IllegalArgumentException("Bounds may only appear on right-hand side");
            }
            vars.add(ext1);
            if(Tools.isTermList(ext2)) {
                bounds.put(ext1, (IStrategoList) ext2);
            } else {
                vars.add(ext2);
                try {
                    closure.add(ext1, ext2);
                } catch(RelationException e) {
                    throw new java.lang.IllegalArgumentException("Constriants violated relation.", e);
                }
            }
        }
        final List<IStrategoTerm> result = new ArrayList<>();
        for(IStrategoTerm ext1 : vars) {
            // @formatter:off
            final List<IStrategoList> bnds2 = closure.larger(ext1).stream()
                    .filter(bounds::containsKey)
                    .flatMap(ext2 -> bounds.get(ext2).stream())
                    .collect(Collectors.toList());
            // @formatter:on
            final IStrategoList bnds = TF.makeList(bnds2);
            final IStrategoList bnd = (IStrategoList) combine.invoke(context, bnds);
            if(bnd == null) {
                continue;
            }
            result.add(TF.makeTuple(ext1, bnd));
        }
        return TF.makeList(result);
    }

}