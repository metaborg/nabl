package mb.nabl2.spoofax.analysis;

import static mb.nabl2.terms.build.TermBuild.B;

import com.google.common.collect.ImmutableClassToInstanceMap;

import mb.nabl2.stratego.ImmutableTermIndex;
import mb.nabl2.stratego.ImmutableTermOrigin;
import mb.nabl2.stratego.TermIndex;
import mb.nabl2.stratego.TermOrigin;
import mb.nabl2.terms.ITerm;

public class Actions {

    public static ITerm sourceTerm(String resource) {
        return sourceTerm(resource, B.newString(resource));
    }

    public static ITerm sourceTerm(String resource, ITerm term) {
        TermIndex index = ImmutableTermIndex.of(resource, 0);
        TermOrigin origin = ImmutableTermOrigin.of(resource);
        ImmutableClassToInstanceMap<Object> attachments =
                ImmutableClassToInstanceMap.builder().put(TermIndex.class, index).put(TermOrigin.class, origin).build();
        return term.withAttachments(attachments);
    }

}
