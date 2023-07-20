package mb.nabl2.relations.variants;

import java.util.Map.Entry;

import org.metaborg.util.collection.CapsuleUtil;

import io.usethesource.capsule.Map;

public class VariantRelations {

    public static <T> Map.Immutable<String, IVariantRelation.Transient<T>>
            melt(java.util.Map<String, IVariantRelation.Immutable<T>> relations) {
        Map.Transient<String, IVariantRelation.Transient<T>> transformed = CapsuleUtil.transientMap();
        for(Entry<String, IVariantRelation.Immutable<T>> entry : relations.entrySet()) {
            transformed.__put(entry.getKey(), entry.getValue().melt());
        }
        return transformed.freeze();
    }

    public static <T> Map.Immutable<String, IVariantRelation.Immutable<T>>
            freeze(java.util.Map<String, IVariantRelation.Transient<T>> relations) {
        Map.Transient<String, IVariantRelation.Immutable<T>> transformed = CapsuleUtil.transientMap();
        for(Entry<String, IVariantRelation.Transient<T>> entry : relations.entrySet()) {
            transformed.__put(entry.getKey(), entry.getValue().freeze());
        }
        return transformed.freeze();
    }

    public static <T> Map.Immutable<String, IVariantRelation.Immutable<T>>
            immutableOf(java.util.Map<String, VariantRelationDescription<T>> descriptions) {
        Map.Transient<String, IVariantRelation.Immutable<T>> transformed = CapsuleUtil.transientMap();
        for(Entry<String, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.__put(entry.getKey(), VariantRelation.Immutable.of(entry.getValue()));
        }
        return transformed.freeze();
    }

    public static <T> Map.Immutable<String, IVariantRelation.Transient<T>>
            transientOf(java.util.Map<String, VariantRelationDescription<T>> descriptions) {
        Map.Transient<String, IVariantRelation.Transient<T>> transformed = CapsuleUtil.transientMap();
        for(Entry<String, VariantRelationDescription<T>> entry : descriptions.entrySet()) {
            transformed.__put(entry.getKey(), VariantRelation.Transient.of(entry.getValue()));
        }
        return transformed.freeze();
    }

}