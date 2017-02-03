package org.metaborg.meta.nabl2.stratego;

import java.io.Serializable;
import java.util.Optional;

import org.metaborg.meta.nabl2.terms.ITermIndex;
import org.spoofax.interpreter.core.Tools;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.terms.StrategoAppl;

public class StrategoTermIndex extends StrategoAppl implements IStrategoTerm, ITermIndex, Serializable {

    private static final long serialVersionUID = 42L;

    private static final String OP = "TermIndex";
    private static final int ARITY = 2;

    private final String resource;
    private final int id;
    
    private StrategoTermIndex(String resource, int id, ITermFactory termFactory) {
        super(termFactory.makeConstructor(OP, ARITY),
                new IStrategoTerm[] {
                    termFactory.makeString(resource),
                    termFactory.makeInt(id)
                },
                termFactory.makeList(),
                termFactory.getDefaultStorageType());
        this.resource = resource;
        this.id = id;
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public int getId() {
        return id;
    }
    
    public static StrategoTermIndex of(String resource, int id, ITermFactory termFactory) {
        return new StrategoTermIndex(resource, id, termFactory);
    }

    public static Optional<StrategoTermIndex> get(IStrategoTerm term, ITermFactory termFactory) {
        for (IStrategoTerm anno : term.getAnnotations()) {
            Optional<StrategoTermIndex> index = match(anno, termFactory);
            if (index.isPresent()) {
                return index;
            }
        }
        return Optional.empty();
    }
 
    public IStrategoTerm put(IStrategoTerm term, ITermFactory termFactory) {
        IStrategoList annos = termFactory.makeListCons(this, term.getAnnotations());
        return termFactory.copyAttachments(term, termFactory.annotateTerm(term, annos));
    }

    public static Optional<StrategoTermIndex> match(IStrategoTerm term, ITermFactory termFactory) {
        if (!Tools.isTermAppl(term)) {
            return Optional.empty();
        }
        IStrategoAppl appl = (IStrategoAppl) term;
        if (!Tools.hasConstructor(appl, OP, ARITY)) {
            return Optional.empty();
        }
        IStrategoTerm resourceTerm = appl.getSubterm(0);
        IStrategoTerm idTerm = appl.getSubterm(1);
        if(!(Tools.isTermString(resourceTerm) && Tools.isTermInt(idTerm))) {
            return Optional.empty();
        }
        return Optional.of(new StrategoTermIndex(Tools.asJavaString(resourceTerm), Tools.asJavaInt(idTerm), termFactory));
    }
    
}