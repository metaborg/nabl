package org.metaborg.meta.nabl2.util.collections;

import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

public final class PSets {

    public static <T> PSet<T> intersection(PSet<T> left, PSet<T> right) {
        if(left.size() > right.size()) {
            return intersection(right, left);
        }
        PSet<T> isect = HashTreePSet.empty();
        for(T elem : left) {
            if(right.contains(elem)) {
                isect = isect.plus(elem);
            }
        }
        return isect;
    }

}