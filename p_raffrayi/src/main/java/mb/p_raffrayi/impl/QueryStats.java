package mb.p_raffrayi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.metaborg.util.tuple.Tuple2;
import org.metaborg.util.tuple.Tuple3;

public class QueryStats<S, L, D> {

    public final List<L> contextDependentScopes = Collections.synchronizedList(new ArrayList<>());

    public final List<Tuple2<S, L>> pendingClose = Collections.synchronizedList(new ArrayList<>());

    public final List<S> pendingData = Collections.synchronizedList(new ArrayList<>());

    public final List<Tuple3<S, D, D>> delayedDataWf = Collections.synchronizedList(new ArrayList<>());

    public final List<Tuple3<S, D, D>> delayedDataLeq = Collections.synchronizedList(new ArrayList<>());

    public final AtomicBoolean local = new AtomicBoolean(true);

}
