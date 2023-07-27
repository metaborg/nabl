package mb.p_raffrayi;

import java.io.PrintStream;
import java.util.Collection;
import java.util.stream.Collectors;

public class PRaffrayiUtil {

    public static <S, L, D> void writeStatsCsvFromResult(IUnitResult<S, L, D, ?> unitResult, PrintStream out) {
        out.println(formatLine("unit", unitResult.stats().csvHeaders()));
        writeStatsCsvDataFromResult(unitResult, out);
    }

    private static <S, L, D> void writeStatsCsvDataFromResult(IUnitResult<S, L, D, ?> unitResult, PrintStream out) {
        out.println(formatLine(unitResult.id(), unitResult.stats().csvRow()));
        for(IUnitResult<S, L, D, ?> subUnitResult : unitResult.subUnitResults().values()) {
            writeStatsCsvDataFromResult(subUnitResult, out);
        }
    }

    private static String formatLine(String id, Collection<String> cells) {
        return id + "," + cells.stream().collect(Collectors.joining(","));
    }

}
