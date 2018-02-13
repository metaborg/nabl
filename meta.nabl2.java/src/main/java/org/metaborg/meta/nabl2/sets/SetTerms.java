package org.metaborg.meta.nabl2.sets;

import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.build.TB;
import org.metaborg.meta.nabl2.terms.matching.Match.IMatcher;
import org.metaborg.meta.nabl2.terms.matching.Match.M;

public class SetTerms {

    private static final String NO_PROJECTION = "NoProjection";
    private static final String PROJECTION = "Projection";

    public static IMatcher<Optional<String>> projectionMatcher() {
        return M.cases(
            // @formatter:off
            M.appl0(NO_PROJECTION, t -> Optional.empty()),
            M.appl1(PROJECTION, M.stringValue(), (t, p) -> Optional.of(p))
            // @formatter:on
        );
    }

    public static ITerm buildProjection(Optional<String> projection) {
        return projection.map(p -> TB.newAppl(PROJECTION, TB.newString(p))).orElseGet(() -> TB.newAppl(NO_PROJECTION));
    }

}
