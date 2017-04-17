package org.metaborg.meta.nabl2.relations.terms;

import org.metaborg.meta.nabl2.relations.RelationDescription.Reflexivity;
import org.metaborg.meta.nabl2.relations.RelationDescription.Symmetry;
import org.metaborg.meta.nabl2.relations.RelationDescription.Transitivity;
import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

public class RelationOptions {

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