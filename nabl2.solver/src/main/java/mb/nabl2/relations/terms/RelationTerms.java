package mb.nabl2.relations.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import mb.nabl2.relations.ImmutableRelationDescription;
import mb.nabl2.relations.RelationDescription;
import mb.nabl2.relations.RelationDescription.Reflexivity;
import mb.nabl2.relations.RelationDescription.Symmetry;
import mb.nabl2.relations.RelationDescription.Transitivity;
import mb.nabl2.relations.terms.RelationName.NamedRelation;
import mb.nabl2.relations.variants.IVariantMatcher;
import mb.nabl2.relations.variants.ImmutableVariantRelationDescription;
import mb.nabl2.relations.variants.VariantRelationDescription;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.nabl2.util.ImmutableTuple3;
import mb.nabl2.util.Tuple3;

public class RelationTerms {

    public static IMatcher<Map<String, VariantRelationDescription<ITerm>>> relations() {
        return M.listElems(relationDef(), (l, defs) -> {
            ImmutableMap.Builder<String, VariantRelationDescription<ITerm>> relations = ImmutableMap.builder();
            for(Tuple3<String, RelationDescription, List<IVariantMatcher<ITerm>>> def : defs) {
                relations.put(def._1(), ImmutableVariantRelationDescription.of(def._2(), def._3()));
            }
            return relations.build();
        });
    }

    public static IMatcher<Tuple3<String, RelationDescription, List<IVariantMatcher<ITerm>>>> relationDef() {
        return M.tuple3(NamedRelation.matcher(), relationDescription(), M.listElems(VariantMatchers.matcher()),
                (t, name, relationDescription, matchers) -> ImmutableTuple3.of(name.getName(), relationDescription,
                        matchers));
    }

    private static IMatcher<RelationDescription> relationDescription() {
        return (term, unifier) -> M.listElems(M.term(), (l, properties) -> {
            Reflexivity refl = Reflexivity.NON_REFLEXIVE;
            Symmetry sym = Symmetry.NON_SYMMETRIC;
            Transitivity trans = Transitivity.NON_TRANSITIVE;
            for(ITerm propTerm : properties) {
                refl = RelationOptions.reflexivity().match(propTerm, unifier).orElse(refl);
                sym = RelationOptions.symmetry().match(propTerm, unifier).orElse(sym);
                trans = RelationOptions.transitivity().match(propTerm, unifier).orElse(trans);
            }
            return (RelationDescription) ImmutableRelationDescription.of(refl, sym, trans);
        }).match(term, unifier);
    }

}