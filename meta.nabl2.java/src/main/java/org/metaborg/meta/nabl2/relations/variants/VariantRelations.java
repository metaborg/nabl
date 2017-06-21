package org.metaborg.meta.nabl2.relations.variants;

import java.util.Map;

import org.metaborg.meta.nabl2.relations.IRelationName;
import org.metaborg.meta.nabl2.relations.RelationException;

import com.google.common.collect.ImmutableMap;

public class VariantRelations {

    public static <T> Map<IRelationName, IVariantRelation.Transient<T>>
            melt(Map<IRelationName, IVariantRelation.Immutable<T>> relations) {
        ImmutableMap.Builder<IRelationName, IVariantRelation.Transient<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<IRelationName, IVariantRelation.Immutable<T>> entry : relations.entrySet()) {
            transformed.put(entry.getKey(), entry.getValue().melt());
        }
        return transformed.build();
    }

    public static <T> Map<IRelationName, IVariantRelation.Immutable<T>>
            freeze(Map<IRelationName, IVariantRelation.Transient<T>> relations) {
        ImmutableMap.Builder<IRelationName, IVariantRelation.Immutable<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<IRelationName, IVariantRelation.Transient<T>> entry : relations.entrySet()) {
            transformed.put(entry.getKey(), entry.getValue().freeze());
        }
        return transformed.build();
    }

    public static <T> Map<IRelationName, IVariantRelation.Transient<T>> extend(
            Map<IRelationName, IVariantRelation.Transient<T>> relations1,
            Map<IRelationName, ? extends IVariantRelation<T>> relations2) throws RelationException {
        ImmutableMap.Builder<IRelationName, IVariantRelation.Transient<T>> relations = ImmutableMap.builder();
        for(Map.Entry<IRelationName, IVariantRelation.Transient<T>> entry : relations1.entrySet()) {
            relations.put(entry.getKey(), VariantRelation.extend(entry.getValue(), relations2.get(entry.getKey())));
        }
        return relations.build();
    }

    public static <T> Map<IRelationName, IVariantRelation.Immutable<T>>
            immutableOf(Map<IRelationName, VariantRelationDescription<T>> descriptions) {
        ImmutableMap.Builder<IRelationName, IVariantRelation.Immutable<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<IRelationName, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.put(entry.getKey(), VariantRelation.Immutable.of(entry.getValue()));
        }
        return transformed.build();
    }

    public static <T> Map<IRelationName, IVariantRelation.Transient<T>>
            transientOf(Map<IRelationName, VariantRelationDescription<T>> descriptions) {
        ImmutableMap.Builder<IRelationName, IVariantRelation.Transient<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<IRelationName, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.put(entry.getKey(), VariantRelation.Transient.of(entry.getValue()));
        }
        return transformed.build();
    }

}