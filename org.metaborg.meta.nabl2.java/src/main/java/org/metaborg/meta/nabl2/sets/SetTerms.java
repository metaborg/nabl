package org.metaborg.meta.nabl2.sets;

import java.util.Optional;

import org.metaborg.meta.nabl2.terms.Terms.IMatcher;
import org.metaborg.meta.nabl2.terms.Terms.M;

public class SetTerms {

    public static <T> IMatcher<Optional<String>> projection() {
        return M.cases(
            // @formatter:off
            M.appl0("NoProjection", t -> Optional.empty()),
            M.appl1("Projection", M.stringValue(), (t, p) -> Optional.of(p))
            // @formatter:on
        );
    }

}
