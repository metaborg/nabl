package org.metaborg.meta.nabl2.util.collections;

import org.pcollections.PSet;

public final class PSets {

    public static <T> boolean intersect(PSet<T> left, T intf, PSet<T> right) {
        if(left.size() > right.size()) {
            return intersect(right, intf, left);
        }
        for(T t : left) {
            if(!t.equals(intf) && right.contains(t)) {
                return true;
            }
        }
        return false;
    }

}