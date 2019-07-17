package mb.statix.taico.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class TTimings {
    public static int runCounter = -1;
    public static LinkedHashMap<Integer, LinkedHashMap<String, PhaseDetails>> results = new LinkedHashMap<>();
    public static LinkedHashMap<Integer, String> runTime = new LinkedHashMap<>();
    public static LinkedHashMap<Integer, List<String>> details = new LinkedHashMap<>();
    public static final CSVFormat format = CSVFormat.EXCEL;
    public static final String FILE = "'results' yyyy.MM.dd 'at' HH:mm:ss";
    public static final String FOLDER = TDebug.DEBUG_FILE_PATH + "/results";
    private static boolean runFixed = false;
    
    /**
     * Prevents starting a new run.
     */
    public static void fixRun() {
        if (runCounter == -1) startNewRun();
        runFixed = true;
    }
    
    /**
     * Stops preventing new runs from being started.
     */
    public static void unfixRun() {
        runFixed = false;
    }
    
    public static void startNewRun() {
        if (runFixed) return;
        
        results.put(++runCounter, new LinkedHashMap<>());
        runTime.put(runCounter, DateFormat.getInstance().format(new Date()));
    }
    
    public static void startPhase(String name, String... details) {
        startPhase(name, System.currentTimeMillis(), details);
    }
    
    public static void startPhase(String name, long time, String... details) {
        System.out.println("Starting phase " + name);
        if (runCounter == -1) startNewRun();
        results.get(runCounter).put(name, new PhaseDetails(time, details));
    }
    
    public static void startPhase(String name, String details, Object... args) {
        startPhase(name, System.currentTimeMillis(), details, args);
    }
    
    public static void startPhase(String name, long time, String details, Object... args) {
        System.out.println("Starting phase " + name);
        if (runCounter == -1) startNewRun();
        results.get(runCounter).put(name, new PhaseDetails(time, String.format(details, args)));
    }
    
    public static void endPhase(String name) {
        endPhase(name, System.currentTimeMillis());
    }
    
    public static void endPhase(String name, long time) {
        long duration = results.get(runCounter).get(name).end(time);
        System.out.println("Completed phase " + name + " in " + duration + " ms");
    }
    
    @Deprecated
    public static void addDetails(String format, Object... args) {
        List<String> runDetails = details.computeIfAbsent(runCounter, i -> new ArrayList<>());
        
        runDetails.add(String.format(format, args));
    }
    
    public static void clear() {
        details.clear();
        results.clear();
        runTime.clear();
        runCounter = -1;
    }
    
    public static void serialize() {
        serialize(getFile());
    }
    
    public static void serialize(File file) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(file), format)) {
            printer.printRecord("run", "phase", "start", "end", "duration", "details");
            for (Entry<Integer, LinkedHashMap<String, PhaseDetails>> entry : results.entrySet()) {
                final Integer run = entry.getKey();
                printer.printRecord(run, runTime.get(run));
                
                for (String s : details.getOrDefault(run, Collections.emptyList())) {
                    printer.printRecord(run, "", 0, 0, 0, s);
                }
                
                for (Entry<String, PhaseDetails> e : entry.getValue().entrySet()) {
                    PhaseDetails d = e.getValue();
                    printer.print(run);
                    printer.print(e.getKey());
                    printer.print(d.start);
                    printer.print(d.end);
                    printer.print(d.duration());
                    for (String s : d.details) {
                        printer.print(s);
                    }
                    printer.println();
                }
                printer.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static File getFile() {
        File folder = new File(FOLDER);
        if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Unable to create folder");
        
        SimpleDateFormat format = new SimpleDateFormat(FILE);
        String name = format.format(System.currentTimeMillis());
        return new File(folder, name + ".csv");
    }
    
    private static class PhaseDetails {
        private long start;
        private long end;
        private String[] details;
        
        public PhaseDetails(long start, String... details) {
            this.start = start;
            this.details = details;
        }
        
        public long duration() {
            return end - start;
        }

        public long end(long endTime) {
            this.end = endTime;
            return duration();
        }
    }
}
