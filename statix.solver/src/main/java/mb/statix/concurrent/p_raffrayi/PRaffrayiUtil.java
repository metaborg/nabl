package mb.statix.concurrent.p_raffrayi;

import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Streams;

public class PRaffrayiUtil {

    public static <S, L, D, R> void writeStatsCsvFromResult(IBrokerResult<S, L, D, R> result, PrintStream out) {
        boolean first = true;
        for(Entry<String, IUnitResult<S, L, D, R>> entry : result.unitResults().entrySet()) {
            if(first) {
                out.println(formatLine("unit", entry.getValue().stats().csvHeaders()));
                first = false;
            }
            System.out.println(formatLine(entry.getKey(), entry.getValue().stats().csvRow()));
        }
    }

    private static String formatLine(String id, Iterable<String> cells) {
        return Streams.stream(Iterables2.cons(id, cells)).collect(Collectors.joining(","));
    }

}