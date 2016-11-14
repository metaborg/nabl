package org.metaborg.meta.nabl2.terms.generic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.metaborg.meta.nabl2.terms.ITerm;
import org.metaborg.meta.nabl2.terms.ITermVar;

public final class Vars {

    @SuppressWarnings("unchecked") public static <T extends ITerm> T newVar(final ITermVar termVar,
            final Class<T> targetClass) {
        return (T) Proxy.newProxyInstance(targetClass.getClassLoader(), new Class<?>[] { targetClass, ITermVar.class },
                new InvocationHandler() {

                    @Override public Object invoke(final Object proxy, final Method method, final Object[] args)
                            throws Throwable {
                        final Class<?> methodClass = method.getDeclaringClass();
                        if (Object.class.equals(methodClass) || ITermVar.class.equals(methodClass)) {
                            return method.invoke(termVar, args);
                        }
                        throw new IllegalStateException("Cannot call methods on non-ground term.");
                    }

                });
    }

    public static boolean isVar(final ITerm term) {
        return term instanceof ITermVar;
    }

    public static ITermVar asVar(final ITerm term) {
        if (!isVar(term)) {
            throw new IllegalArgumentException("Term must be a variable.");
        }
        return (ITermVar) term;
    }

}