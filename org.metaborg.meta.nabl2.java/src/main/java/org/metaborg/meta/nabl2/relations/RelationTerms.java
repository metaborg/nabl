package org.metaborg.meta.nabl2.relations;

import java.util.Map;

import org.metaborg.meta.nabl2.collections.ImmutableTuple2;
import org.metaborg.meta.nabl2.collections.Tuple2;
import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.relations.terms.RelationName;
import org.metaborg.meta.nabl2.solver.Relations;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

import com.google.common.collect.Maps;

public class RelationTerms {

    public static IMatcher<Relations> relations() {
        return M.listElems(relation(), (t, relations) -> {
            Map<RelationName,Relation<ITerm>> r = Maps.newHashMap();
            for (Tuple2<RelationName,Relation<ITerm>> rel : relations) {
                r.put(rel._1(), rel._2());
            }
            return new Relations(r);
        });
    }

    public static IMatcher<Tuple2<RelationName,Relation<ITerm>>> relation() {
        return M.tuple2(RelationName.matcher(), M.listElems(), (term, name, properties) -> {
            Reflexivity refl = Reflexivity.NON_REFLEXIVE;
            Symmetry sym = Symmetry.NON_SYMMETRIC;
            Transitivity trans = Transitivity.NON_TRANSITIVE;
            for (ITerm propTerm : properties) {
                refl = reflexivity().match(propTerm).orElse(refl);
                sym = symmetry().match(propTerm).orElse(sym);
                trans = transitivity().match(propTerm).orElse(trans);
            }
            return ImmutableTuple2.of(name, new Relation<ITerm>(ImmutableRelationDescription.of(refl, sym, trans)));
        });
    }

    public static IMatcher<Reflexivity> reflexivity() {
        return M.<Reflexivity> cases(
            // @formatter:off
            M.appl0("Reflexive", (t) -> Reflexivity.REFLEXIVE),
            M.appl0("Irreflexive", (t) -> Reflexivity.IRREFLEXIVE)
            // @formatter:on
        );
    }

    public static IMatcher<Symmetry> symmetry() {
        return M.<Symmetry> cases(
            // @formatter:off
            M.appl0("Symmetric", (t) -> Symmetry.SYMMETRIC),
            M.appl0("AntiSymmetric", (t) -> Symmetry.ANTI_SYMMETRIC)
            // @formatter:on
        );
    }

    public static IMatcher<Transitivity> transitivity() {
        return M.<Transitivity> cases(
            // @formatter:off
            M.appl0("Transitive", (t) -> Transitivity.TRANSITIVE),
            M.appl0("AntiTransitive", (t) -> Transitivity.ANTI_TRANSITIVE)
            // @formatter:on
        );
    }

}