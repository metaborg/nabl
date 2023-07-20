package mb.nabl2.relations.terms;

import static mb.nabl2.terms.matching.TermMatch.M;

import java.util.List;

import org.metaborg.util.collection.CapsuleUtil;
import org.metaborg.util.tuple.Tuple3;

import io.usethesource.capsule.Map;
import mb.nabl2.relations.variants.IVariantMatcher;
import mb.nabl2.relations.variants.VariantRelationDescription;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.matching.TermMatch.IMatcher;
import mb.scopegraph.relations.ARelationDescription.Reflexivity;
import mb.scopegraph.relations.ARelationDescription.Symmetry;
import mb.scopegraph.relations.ARelationDescription.Transitivity;
import mb.scopegraph.relations.RelationDescription;

public class RelationTerms {

    public static IMatcher<Map.Immutable<String, VariantRelationDescription<ITerm>>> relations() {
        return M.listElems(relationDef(), (l, defs) -> {
            Map.Transient<String, VariantRelationDescription<ITerm>> relations = CapsuleUtil.transientMap();
            for(Tuple3<String, RelationDescription, List<IVariantMatcher<ITerm>>> def : defs) {
                relations.__put(def._1(), VariantRelationDescription.of(def._2(), def._3()));
            }
            return relations.freeze();
        });
    }

    public static IMatcher<Tuple3<String, RelationDescription, List<IVariantMatcher<ITerm>>>> relationDef() {
        return M.tuple3(NamedRelation.matcher(), relationDescription(), M.listElems(VariantMatchers.matcher()),
                (t, name, relationDescription, matchers) -> Tuple3.of(name.getName(), relationDescription,
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
            return (RelationDescription) RelationDescription.of(refl, sym, trans);
        }).match(term, unifier);
    }

}