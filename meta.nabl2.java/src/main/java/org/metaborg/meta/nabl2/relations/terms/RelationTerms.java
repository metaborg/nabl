package org.metaborg.meta.nabl2.relations.terms;

import static org.metaborg.meta.nabl2.terms.matching.TermMatch.M;

import java.util.Map;

import org.metaborg.meta.nabl2.relations.ImmutableRelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.relations.terms.RelationName.NamedRelation;
import org.metaborg.meta.nabl2.relations.variants.IVariantMatcher;
import org.metaborg.meta.nabl2.relations.variants.ImmutableVariantRelationDescription;
import org.metaborg.meta.nabl2.relations.variants.VariantRelationDescription;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.matching.TermMatch.IMatcher;
import org.metaborg.meta.nabl2.util.ImmutableTuple3;
import org.metaborg.meta.nabl2.util.Tuple3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class RelationTerms {

    public static IMatcher<Map<String, VariantRelationDescription<ITerm>>> relations() {
        return M.listElems(relationDef(), (l, defs) -> {
            ImmutableMap.Builder<String, VariantRelationDescription<ITerm>> relations = ImmutableMap.builder();
            for(Tuple3<String, RelationDescription, ImmutableList<IVariantMatcher<ITerm>>> def : defs) {
                relations.put(def._1(), ImmutableVariantRelationDescription.of(def._2(), def._3()));
            }
            return relations.build();
        });
    }

    public static IMatcher<Tuple3<String, RelationDescription, ImmutableList<IVariantMatcher<ITerm>>>> relationDef() {
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