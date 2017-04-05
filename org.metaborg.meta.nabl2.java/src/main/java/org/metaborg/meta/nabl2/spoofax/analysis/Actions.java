package org.metaborg.meta.nabl2.spoofax.analysis;

import org.metaborg.meta.nabl2.stratego.ImmutableTermIndex;
import org.metaborg.meta.nabl2.stratego.ImmutableTermOrigin;
import org.metaborg.meta.nabl2.stratego.TermIndex;
import org.metaborg.meta.nabl2.stratego.TermOrigin;
import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.TB;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;

public class Actions {

    public static ITerm analyzeInitial(String resource) {
        return TB.newAppl("AnalyzeInitial", sourceTerm(resource));
    }

    public static ITerm analyzeUnit(String resource, ITerm ast, Args args) {
        return TB.newAppl("AnalyzeUnit", sourceTerm(resource), ast, args(args));
    }

    public static ITerm analyzeFinal(String resource) {
        return TB.newAppl("AnalyzeFinal", sourceTerm(resource));
    }

    public static ITerm customInitial(String resource) {
        return TB.newAppl("CustomInitial", sourceTerm(resource));
    }

    public static ITerm customUnit(String resource, ITerm ast, ITerm initial) {
        return TB.newAppl("CustomUnit", sourceTerm(resource), ast, initial);
    }

    public static ITerm customFinal(String resource, ITerm initial, Iterable<ITerm> units) {
        return TB.newAppl("CustomFinal", sourceTerm(resource), initial, TB.newList(units));
    }

    public static ITerm sourceTerm(String resource) {
        TermIndex index = ImmutableTermIndex.of(resource, 0);
        TermOrigin origin = ImmutableTermOrigin.of(resource, 0, 0, 0, 0, 0, 0);
        return TB.newString(resource, ImmutableClassToInstanceMap.builder()
            .put(TermIndex.class, index)
            .put(TermOrigin.class, origin)
            .build());
    }

    private static ITerm args(Args args) {
        Iterable<ITerm> paramTerms = args.getParams();
        ITerm paramsTerm;
        if(Iterables.size(paramTerms) == 1) {
            paramsTerm = Iterables.getOnlyElement(paramTerms);
        } else {
            paramsTerm = TB.newTuple(paramTerms);
        }
        return args.getType()
                // @formatter:off
                .map(typeTerm -> TB.newAppl("ParamsAndType", paramsTerm, typeTerm))
                .orElseGet(() -> TB.newAppl("Params", paramsTerm));
                // @formatter:on
    }

}