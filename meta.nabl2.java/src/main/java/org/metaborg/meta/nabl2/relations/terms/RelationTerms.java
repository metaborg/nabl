package org.metaborg.meta.nabl2.relations.terms;

import org.metaborg.meta.nabl2.relations.IRelation;
import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.IRelations;
import org.metaborg.meta.nabl2.relations.IVariantMatcher;
import org.metaborg.meta.nabl2.relations.ImmutableRelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;
import org.metaborg.meta.nabl2.util.tuples.ImmutableTuple3;
import org.metaborg.meta.nabl2.util.tuples.Tuple3;

import com.google.common.collect.ImmutableList;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.SetMultimap;

public class RelationTerms {

    public enum RelationFunctions {
        LUB, GLB
    }

    public static IMatcher<IRelations.Immutable<ITerm>> relations() {
        return M.listElems(relationDef(), (l, defs) -> {
            Map.Transient<IRelationName, IRelation.Immutable<ITerm>> relations = Map.Transient.of();
            SetMultimap.Transient<IRelationName, IVariantMatcher<ITerm>> variantMatchers = SetMultimap.Transient.of();
            for(Tuple3<IRelationName, RelationDescription, ImmutableList<IVariantMatcher<ITerm>>> def : defs) {
                relations.__put(def._1(), Relation.Immutable.of(def._2()));
                def._3().stream().forEach(vm -> variantMatchers.__insert(def._1(), vm)); // FIXME Should be __insertAll
            }
            return new Relations.Immutable<>(relations.freeze(), variantMatchers.freeze());
        });
    }

    public static IMatcher<Tuple3<IRelationName, RelationDescription, ImmutableList<IVariantMatcher<ITerm>>>>
            relationDef() {
        return M.tuple3(RelationName.matcher(), relationDescription(), M.listElems(VariantMatchers.matcher()),
                (t, name, relationDescription, matchers) -> ImmutableTuple3.of(name, relationDescription, matchers));
    }

    private static IMatcher<RelationDescription> relationDescription() {
        return M.listElems((l, properties) -> {
            Reflexivity refl = Reflexivity.NON_REFLEXIVE;
            Symmetry sym = Symmetry.NON_SYMMETRIC;
            Transitivity trans = Transitivity.NON_TRANSITIVE;
            for(ITerm propTerm : properties) {
                refl = RelationOptions.reflexivity().match(propTerm).orElse(refl);
                sym = RelationOptions.symmetry().match(propTerm).orElse(sym);
                trans = RelationOptions.transitivity().match(propTerm).orElse(trans);
            }
            return ImmutableRelationDescription.of(refl, sym, trans);
        });
    }

    public static IMatcher<String> functionName() {
        return M.<String>cases(
            // @formatter:off
            M.appl1("Lub", RelationName.matcher(), (t, r) -> relationFunction(r, RelationFunctions.LUB)),
            M.appl1("Glb", RelationName.matcher(), (t, r) -> relationFunction(r, RelationFunctions.GLB)),
            M.appl1("Function", M.stringValue(), (t, f) -> f)
            // @formatter:on
        );
    }

    public static String relationFunction(IRelationName rel, RelationFunctions fun) {
        return rel.getName().map(n -> n + "." + fun.name()).orElse(fun.name());
    }

}
