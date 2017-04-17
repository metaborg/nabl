package org.metaborg.meta.nabl2.util;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.metaborg.meta.nabl2.util.functions.Function2;
import org.metaborg.meta.nabl2.util.functions.Function3;
import org.metaborg.meta.nabl2.util.functions.Function4;
import org.metaborg.meta.nabl2.util.functions.Function5;

import com.google.common.collect.Lists;

public class Optionals {

    public static <T1, T2, R> Optional<R> lift(Optional<T1> o1, Optional<T2> o2,
            Function2<? super T1, ? super T2, R> f) {
        return (o1.isPresent() && o2.isPresent()) ? Optional.of(f.apply(o1.get(), o2.get())) : Optional.empty();
    }

    public static <T1, T2, T3, R> Optional<R> lift(Optional<T1> o1, Optional<T2> o2, Optional<T3> o3,
            Function3<? super T1, ? super T2, ? super T3, R> f) {
        return (o1.isPresent() && o2.isPresent() && o3.isPresent()) ? Optional.of(f.apply(o1.get(), o2.get(), o3.get()))
                : Optional.empty();
    }

    public static <T1, T2, T3, T4, R> Optional<R> lift(Optional<T1> o1, Optional<T2> o2, Optional<T3> o3,
            Optional<T4> o4, Function4<? super T1, ? super T2, ? super T3, ? super T4, R> f) {
        return (o1.isPresent() && o2.isPresent() && o3.isPresent() && o4.isPresent())
                ? Optional.of(f.apply(o1.get(), o2.get(), o3.get(), o4.get())) : Optional.empty();
    }

    public static <T1, T2, T3, T4, T5, R> Optional<R> lift(Optional<T1> o1, Optional<T2> o2, Optional<T3> o3,
            Optional<T4> o4, Optional<T5> o5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, R> f) {
        return (o1.isPresent() && o2.isPresent() && o3.isPresent() && o4.isPresent() && o5.isPresent())
                ? Optional.of(f.apply(o1.get(), o2.get(), o3.get(), o4.get(), o5.get())) : Optional.empty();
    }

    public static <T> Optional<Iterable<T>> sequence(Iterable<Optional<T>> os) {
        List<T> ts = Lists.newArrayList();
        for(Optional<? extends T> o : os) {
            if(!o.isPresent()) {
                return Optional.empty();
            }
            ts.add(o.get());
        }
        return Optional.of(ts);
    }

    public static <T> Iterable<T> filter(Iterable<Optional<T>> os) {
        List<T> ts = Lists.newArrayList();
        for(Optional<? extends T> o : os) {
            o.ifPresent(ts::add);
        }
        return ts;
    }

    public static <T> Stream<T> filter(Stream<Optional<T>> os) {
        return os.filter(Optional::isPresent).map(Optional::get);
    }

    public static Optional<Unit> when(boolean cond) {
        return cond ? Optional.of(Unit.unit) : Optional.empty();
    }

    public static <T> Stream<T> stream(Optional<T> opt) {
        return opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
    }

}