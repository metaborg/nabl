package org.metaborg.meta.nabl2.scopegraph;

import org.metaborg.meta.nabl2.regexp.IAlphabet;
import org.metaborg.meta.nabl2.regexp.IRegExp;
import org.metaborg.meta.nabl2.relations.IRelation;

public interface IResolutionParameters<L extends ILabel> {

    L getLabelD();

    IAlphabet<L> getLabels();

    IRegExp<L> getPathWf();

    IRelation<L> getSpecificityOrder();

}