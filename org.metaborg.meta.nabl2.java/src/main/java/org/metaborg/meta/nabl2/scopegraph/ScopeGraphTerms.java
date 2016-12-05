package org.metaborg.meta.nabl2.scopegraph;

import static org.metaborg.meta.nabl2.terms.TermMatchers.*;

import org.metaborg.meta.nabl2.functions.PartialFunction1;
import org.metaborg.meta.nabl2.terms.*;

public class ScopeGraphTerms {

    public static PartialFunction1<ITerm,Scope> scope() {
        return appl("Scope", string(), string(), (resource, name) -> ImmutableScope.of(resource, name));
    }

    public static PartialFunction1<ITerm,Occurrence> occurrence() {
        return appl("Occurrence", any(), any(), termIndex(),
                (namespace, name, termIndex) -> ImmutableOccurrence.of(namespace().apply(namespace), name, termIndex));
    }

    public static PartialFunction1<ITerm,TermIndex> termIndex() {
        return appl("TermIndex", string(), integer(), (resource, id) -> ImmutableTermIndex.of(resource, id));
    }

    public static PartialFunction1<ITerm,String> namespace() {
        return appl("Namespace", string(), (ns) -> ns);
    }

    public static PartialFunction1<ITerm,Label> label() {
        return PartialFunction1.cases(
            // @formatter:off
            appl("P", () ->
                ImmutableLabel.of("P")),
            appl("I", () ->
                ImmutableLabel.of("I")),
            appl("Label", string(), (l) ->
                ImmutableLabel.of(l))
            // @formatter:on
        );
    }

}