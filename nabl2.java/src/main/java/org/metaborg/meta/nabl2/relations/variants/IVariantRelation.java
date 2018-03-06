package org.metaborg.meta.nabl2.relations.variants;

import org.metaborg.meta.nabl2.relations.IRelation;

public interface IVariantRelation<T> extends IRelation<T> {

    Iterable<IVariantMatcher<T>> getVariantMatchers();

    interface Immutable<T> extends IVariantRelation<T>, IRelation.Immutable<T> {

        @Override IVariantRelation.Transient<T> melt();

    }

    interface Transient<T> extends IVariantRelation<T>, IRelation.Transient<T> {

        @Override IVariantRelation.Immutable<T> freeze();

    }

}
