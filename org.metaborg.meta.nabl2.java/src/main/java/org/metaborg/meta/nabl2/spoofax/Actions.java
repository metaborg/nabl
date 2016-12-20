package org.metaborg.meta.nabl2.spoofax;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.generic.GenericTerms;
import org.metaborg.meta.nabl2.terms.generic.ImmutableTermIndex;
import org.metaborg.meta.nabl2.terms.generic.TermIndex;
import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.Iterables;

public class Actions {

    public static ITerm analyzeInitial(String resource) {
        return GenericTerms.newAppl("AnalyzeInitial", Iterables2.singleton(sourceTerm(resource)));
    }

    public static ITerm analyzeUnit(String resource, ITerm ast, Args args) {
        return GenericTerms.newAppl("AnalyzeUnit", Iterables2.from(sourceTerm(resource), ast, args(args)));
    }

    public static ITerm analyzeFinal(String resource) {
        return GenericTerms.newAppl("AnalyzeFinal", Iterables2.singleton(sourceTerm(resource)));
    }


    public static ITerm customInitial(String resource) {
        return GenericTerms.newAppl("CustomInitial", Iterables2.singleton(sourceTerm(resource)));
    }

    public static ITerm customUnit(String resource, ITerm ast, ITerm initial) {
        return GenericTerms.newAppl("CustomUnit", Iterables2.from(sourceTerm(resource), ast, initial));
    }

    public static ITerm customFinal(String resource, ITerm initial, Iterable<ITerm> units) {
        return GenericTerms.newAppl("CustomFinal", Iterables2.from(sourceTerm(resource), initial, GenericTerms.newList(
                units)));
    }


    private static ITerm sourceTerm(String resource) {
        return GenericTerms.newString(resource, ImmutableClassToInstanceMap.builder().put(TermIndex.class,
                ImmutableTermIndex.of(resource, 0)).build());
    }

    private static ITerm args(Args args) {
        Iterable<ITerm> paramTerms = args.getParams();
        ITerm paramsTerm;
        if (Iterables.size(paramTerms) == 1) {
            paramsTerm = Iterables.getOnlyElement(paramTerms);
        } else {
            paramsTerm = GenericTerms.newTuple(paramTerms);
        }
        return args.getType()
                // @formatter:off
                .map(typeTerm -> GenericTerms.newAppl("ParamsAndType", Iterables2.from(paramsTerm, typeTerm)))
                .orElseGet(() -> GenericTerms.newAppl("Params", Iterables2.singleton(paramsTerm)));
                // @formatter:on
    }

}