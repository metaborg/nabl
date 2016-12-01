package org.metaborg.meta.nabl2.stratego;

import java.util.List;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermFactory;
import org.metaborg.meta.nabl2.terms.Terms;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoConstructor;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.common.collect.Lists;

public class StrategoCommon {

    private final static String VAR_CTOR = "CVar";
    private final static int VAR_ARITY = 2;

    private final ITermFactory termFactory;

    private final org.spoofax.interpreter.terms.ITermFactory strategoTermFactory;
    private IStrategoConstructor varCtor;

    public StrategoCommon(ITermFactory termFactory, org.spoofax.interpreter.terms.ITermFactory strategoTermFactory) {
        this.termFactory = termFactory;
        this.strategoTermFactory = strategoTermFactory;
        this.varCtor = strategoTermFactory.makeConstructor(VAR_CTOR, VAR_ARITY);
    }

    public IStrategoTerm toStratego(ITerm term) {
        return term.match(Terms.<IStrategoTerm> cases(
            // @formatter:off
            appl -> strategoTermFactory.makeAppl(strategoTermFactory.makeConstructor(appl.getOp(), appl.getArity()),
                        toStrategos(appl.getArgs()).toArray(new IStrategoTerm[0])),
            tuple -> strategoTermFactory.makeTuple(toStrategos(tuple.getArgs()).toArray(new IStrategoTerm[0])),
            list -> strategoTermFactory.makeList(toStrategos(list)),
            string -> strategoTermFactory.makeString(string.getValue()),
            integer -> strategoTermFactory.makeInt(integer.getValue()),
            var -> strategoTermFactory.makeAppl(varCtor, strategoTermFactory.makeString(var.getName()))
            // @formatter:on
        ));
    }

    private List<IStrategoTerm> toStrategos(Iterable<ITerm> terms) {
        List<IStrategoTerm> strategoTerms = Lists.newArrayList();
        for (ITerm term : terms) {
            strategoTerms.add(toStratego(term));
        }
        return strategoTerms;
    }

    public ITerm fromStratego(IStrategoTerm term) {
        return StrategoTerms.match(term,
                StrategoTerms.<ITerm> cases(
            // @formatter:off
            appl -> StrategoMatchers.<ITerm>patterns()
                        .appl2(VAR_CTOR, (resource, name) -> termFactory.newVar(Tools.asJavaString(resource),Tools.asJavaString(name)))
                        .otherwise(() -> termFactory.newAppl(appl.getConstructor().getName(), fromStrategos(appl)))
                        .match(appl),
            tuple -> termFactory.newTuple(fromStrategos(tuple)),
            list -> termFactory.newList(fromStrategos(list)),
            integer -> termFactory.newInt(integer.intValue()),
            string -> termFactory.newString(string.stringValue())
            // @formatter:on
                ));
    }

    public Iterable<ITerm> fromStrategos(Iterable<IStrategoTerm> strategoTerms) {
        List<ITerm> terms = Lists.newArrayList();
        for (IStrategoTerm strategoTerm : strategoTerms) {
            terms.add(fromStratego(strategoTerm));
        }
        return terms;
    }

}