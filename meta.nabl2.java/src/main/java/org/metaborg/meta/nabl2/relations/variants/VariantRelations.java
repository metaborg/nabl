package org.metaborg.meta.nabl2.relations.variants;

import java.util.Map;

import org.metaborg.meta.nabl2.relations.RelationException;

import com.google.common.collect.ImmutableMap;

public class VariantRelations {

    public static <T> Map<String, IVariantRelation.Transient<T>>
            melt(Map<String, IVariantRelation.Immutable<T>> relations) {
        ImmutableMap.Builder<String, IVariantRelation.Transient<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<String, IVariantRelation.Immutable<T>> entry : relations.entrySet()) {
            transformed.put(entry.getKey(), entry.getValue().melt());
        }
        return transformed.build();
    }

    public static <T> Map<String, IVariantRelation.Immutable<T>>
            freeze(Map<String, IVariantRelation.Transient<T>> relations) {
        ImmutableMap.Builder<String, IVariantRelation.Immutable<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<String, IVariantRelation.Transient<T>> entry : relations.entrySet()) {
            transformed.put(entry.getKey(), entry.getValue().freeze());
        }
        return transformed.build();
    }

    public static <T> Map<String, IVariantRelation.Transient<T>> extend(
            Map<String, IVariantRelation.Transient<T>> relations1,
            Map<String, ? extends IVariantRelation<T>> relations2) throws RelationException {
        ImmutableMap.Builder<String, IVariantRelation.Transient<T>> relations = ImmutableMap.builder();
        for(Map.Entry<String, IVariantRelation.Transient<T>> entry : relations1.entrySet()) {
            relations.put(entry.getKey(), VariantRelation.extend(entry.getValue(), relations2.get(entry.getKey())));
        }
        return relations.build();
    }

    public static <T> Map<String, IVariantRelation.Immutable<T>>
            immutableOf(Map<String, VariantRelationDescription<T>> descriptions) {
        ImmutableMap.Builder<String, IVariantRelation.Immutable<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<String, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.put(entry.getKey(), VariantRelation.Immutable.of(entry.getValue()));
        }
        return transformed.build();
    }

    public static <T> Map<String, IVariantRelation.Transient<T>>
            transientOf(Map<String, VariantRelationDescription<T>> descriptions) {
        ImmutableMap.Builder<String, IVariantRelation.Transient<T>> transformed = ImmutableMap.builder();
        for(Map.Entry<String, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.put(entry.getKey(), VariantRelation.Transient.of(entry.getValue()));
        }
        return transformed.build();
    }

}