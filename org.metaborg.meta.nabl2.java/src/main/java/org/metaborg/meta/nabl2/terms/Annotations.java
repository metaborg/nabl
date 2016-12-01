package org.metaborg.meta.nabl2.terms;

import com.google.common.collect.ImmutableClassToInstanceMap;

public class Annotations {

    public static ImmutableClassToInstanceMap<IAnnotation> empty() {
        return ImmutableClassToInstanceMap.<IAnnotation> builder().build();
    }

}