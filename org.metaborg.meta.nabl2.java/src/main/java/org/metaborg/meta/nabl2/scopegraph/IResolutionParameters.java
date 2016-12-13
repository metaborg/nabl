package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.transitiveclosure.TransitiveClosure;

public interface IResolutionParameters<L extends ILabel> {

    IAlphabet<L> getLabels();
    
    IRegExp<L> getPathWf();
    
    TransitiveClosure<L> getSpecificityOrder();
    
}