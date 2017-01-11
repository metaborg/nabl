package org.metaborg.meta.nabl2.stratego;

import org.metaborg.meta.nabl2.ScopeGraphException;
import org.spoofax.interpreter.terms.IStrategoList;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.visitor.AStrategoTermVisitor;
import org.spoofax.terms.visitor.StrategoTermVisitee;

public class StrategoTermIndices {

    public static void indexTerm(final String resource, final IStrategoTerm term) {
        StrategoTermVisitee.topdown(new AStrategoTermVisitor() {

            private int currentId = 0;

            @Override public boolean visit(IStrategoList list) {
                visit((IStrategoTerm) list);
                currentId += list.size();
                return true;
            }

            @Override public boolean visit(IStrategoTerm term) {
                StrategoTermIndex.put(term, resource, ++currentId);
                return true;
            }

        }, term);
    }

    public static void indexSublist(IStrategoTerm list, IStrategoTerm sublist) throws ScopeGraphException {
        if (!list.isList()) {
            throw new ScopeGraphException("List term is not a list.");
        }
        if (!sublist.isList()) {
            throw new ScopeGraphException("Sublist term is not a list.");
        }
        int listCount = list.getSubtermCount();
        int sublistCount = sublist.getSubtermCount();
        if (sublistCount > listCount) {
            throw new ScopeGraphException("Sublist cannot be longer than original list.");
        }

        StrategoTermIndex index = StrategoTermIndex.get(list);
        if (index == null) {
            throw new ScopeGraphException("List has no index.");
        }

        int skip = listCount - sublistCount;
        StrategoTermIndex.put(sublist, index.getResource(), index.getId() + skip);
    }

}