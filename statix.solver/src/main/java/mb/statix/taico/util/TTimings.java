package mb.statix.taico.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class TTimings {
    public static int runCounter = -1;
    public static LinkedHashMap<Integer, LinkedHashMap<String, Long>> results = new LinkedHashMap<>();
    public static LinkedHashMap<Integer, String> details = new LinkedHashMap<>();
    public static final CSVFormat format = CSVFormat.EXCEL;
    public static final String FILE = "'results' yyyy.MM.dd 'at' HH:mm:ss";
    //public static final String FOLDER = "~";
    
    public static void startNewRun() {
        results.put(++runCounter, new LinkedHashMap<>());
    }
    
    public static void startPhase(String name) {
        startPhase(name, System.currentTimeMillis());
    }
    
    public static void startPhase(String name, long time) {
        results.get(runCounter).put(name, time);
    }
    
    public static void endPhase(String name) {
        endPhase(name, System.currentTimeMillis());
    }
    
    public static void endPhase(String name, long time) {
        results.get(runCounter).compute(name, (k, oldV) -> time - oldV);
    }
    
    public static void addDetails(String format, Object... args) {
        details.put(runCounter, String.format(format, args));
    }
    
    public static void serialize() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(getFile()), format)) {
            printer.printRecord("run", "phase", "duration");
            for (Entry<Integer, LinkedHashMap<String, Long>> entry : results.entrySet()) {
                final Integer phase = entry.getKey();
                printer.printRecord(phase, details.getOrDefault(phase, "no details"), 0);
                printer.println();
                
                for (Entry<String, Long> e : entry.getValue().entrySet()) {
                    printer.printRecord(phase, e.getKey(), e.getValue());
                }
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static File getFile() {
        SimpleDateFormat format = new SimpleDateFormat(FILE);
        String name = format.format(System.currentTimeMillis());
        return new File("~", name + ".csv");
    }
}
