package mb.nabl2.spoofax.primitives;

import java.util.Map;
import java.util.Optional;

import jakarta.annotation.Nullable;

import org.spoofax.interpreter.core.IContext;
import org.spoofax.interpreter.core.Interpreter;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.core.UndefinedStrategyException;
import org.spoofax.interpreter.stratego.SDefT;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.lang.InteropContext;

public class StrategyCalls {

    public static CallableStrategy lookup(IContext env, String name) throws InterpreterException {
        return lookup(env, name, null);
    }

    public static CallableStrategy lookup(IContext env, String name, @Nullable Map<String, SDefT> strCache)
            throws InterpreterException {
        if(env instanceof InteropContext) {
            return lookupStrategyJar((InteropContext) env, name);
        } else {
            return lookupStrategyCtree(env, name, strCache);
        }
    }

    private static CallableStrategy lookupStrategyJar(InteropContext context, String name) throws InterpreterException {
        final HybridInterpreter interpreter = HybridInterpreter.getInterpreter(context.getContext());
        if(interpreter == null) {
            throw new InterpreterException("Cannot get interpreter.");
        }
        return arg -> {
            final IStrategoTerm prev = interpreter.current();
            interpreter.setCurrent(arg);
            try {
                if(!interpreter.invoke(name)) {
                    return Optional.empty();
                }
                return Optional.of(interpreter.current());
            } finally {
                interpreter.setCurrent(prev);
            }
        };
    }

    private static CallableStrategy lookupStrategyCtree(IContext env, final String name,
            @Nullable Map<String, SDefT> strCache) throws InterpreterException {
        final String cname = Interpreter.cify(name) + "_0_0";
        final SDefT s;
        if(strCache != null && strCache.containsKey(cname)) {
            s = strCache.get(cname);
        } else {
            s = env.lookupSVar(cname);
            if(s == null) {
                throw new UndefinedStrategyException("Strategy lookup failed.", name);
            }
            if(strCache != null) {
                strCache.put(cname, s);
            }
        }
        return arg -> {
            final IStrategoTerm prev = env.current();
            env.setCurrent(arg);
            try {
                if(!s.evaluate(env)) {
                    return Optional.empty();
                }
                return Optional.of(env.current());
            } finally {
                env.setCurrent(prev);
            }
        };
    }

    public interface CallableStrategy {

        Optional<IStrategoTerm> call(IStrategoTerm args) throws InterpreterException;

    }

}
