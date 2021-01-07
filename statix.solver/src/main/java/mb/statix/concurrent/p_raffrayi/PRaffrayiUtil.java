package mb.statix.concurrent.p_raffrayi;

import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

import org.metaborg.util.iterators.Iterables2;

import com.google.common.collect.Streams;

public class PRaffrayiUtil {

    public static <S, L, D> void writeStatsCsvFromResult(Collection<? extends IUnitResult<S, L, D, ?>> unitResults,
            PrintStream out) {
        boolean first = true;
        for(IUnitResult<S, L, D, ?> unitResult : unitResults) {
            if(first) {
                out.println(formatLine("unit", unitResult.stats().csvHeaders()));
                first = false;
            }
            System.out.println(formatLine(unitResult.id(), unitResult.stats().csvRow()));
        }
    }

    private static String formatLine(String id, Iterable<String> cells) {
        return Streams.stream(Iterables2.cons(id, cells)).collect(Collectors.joining(","));
    }

}