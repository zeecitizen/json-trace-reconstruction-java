package logparser;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Parser {

    private static Logger log = Logger.getLogger(Main.class.getName());

    Parser() {
    }

    void parseLog(String inputFile, String outputFileName, Boolean usingFilesForIO) throws IOException, NullPointerException {

        long executionStartTime = System.currentTimeMillis();
        String logs = readLogs(inputFile, usingFilesForIO);
        if (logs.equals("")) return;

        String[] logLines = logs.split("\n");

        List<LogEntry> logEntries = new ArrayList<>();
        double averageSize = 0d;
        double averageDepth = 0d;

        double logLineLengthSum = 0;
        double logDepthSum = 0;
        int count = 0;

        for (String line : logLines) {
            logLineLengthSum += line.length();
            LogEntry logEntry = populateLogEntryFromLine(line, ++count);
            logEntries.add(logEntry);
        }

        for (LogEntry e : logEntries) {
            e.setCalls(findLogEntriesWithCallerSpan(logEntries, e.getSpan()));
        }

        for (LogEntry e : logEntries) {
            logDepthSum += e.getCalls().size();
        }

        List<String> traceIds = getUniqueTraceIds(logEntries);
        log.info("List of found trace ids: " + traceIds.toString());
        String jsonString = "";

        Gson gson = getGsonBuilder();

        for (String traceId : traceIds) {
            LogEntry rootEntry = findNullRootLogEntry(logEntries, traceId);
            if (rootEntry == null) continue; //ignore orphans
            jsonString = makeAdjustments(jsonString, gson, rootEntry, traceId);
        }
        log.info(jsonString);

        int orphanLineCount = orphanLineCount(logEntries, jsonString);
        averageDepth = logDepthSum / logEntries.size();
        averageSize = logLineLengthSum / logEntries.size();

        long executionEndTime = System.currentTimeMillis();

        long executionTimeInSecs = executionEndTime - executionStartTime;

        output(usingFilesForIO, executionTimeInSecs, orphanLineCount, averageDepth, averageSize, outputFileName, jsonString, inputFile);
    }

    private String makeAdjustments(String jsonString, Gson gson, LogEntry rootEntry, String traceId) {
        String emptyArrayJson = ",\n\\s{2,}\"calls\": \\[\\]\n";
        try {
            jsonString += gson.toJson(rootEntry).replaceAll(emptyArrayJson, "\n").replaceFirst("\\{\n", "{\"id\": \"" + traceId + "\",\n\"root\": {\n") + "}\n";
        } catch (Exception e) {
            log.error("MakeAdjustments function failed.", e);
        }
        return jsonString;
    }

    private int orphanLineCount(List<LogEntry> logEntries, String jsonString) {
        int orphanLineCount = 0;
        for (int i = 0; i < logEntries.size(); i++) {
            if (!jsonString.contains(logEntries.get(i).getServiceName())) {
                orphanLineCount++;
            }
        }
        return orphanLineCount;
    }

    private Gson getGsonBuilder() {
        Gson gson = null;
        try {
            GsonBuilder builder = new GsonBuilder();
            builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
            builder.setPrettyPrinting();
            gson = builder.create();
        } catch (Exception e) {
            log.error("Error creating Gson Builder.", e);
        }
        return gson;
    }

    private String readLogs(String inputFile, Boolean usingFilesForIO) throws IOException {
        String logs = "";
        if (usingFilesForIO) {
            logs = readLogsFromFile(inputFile);
            if (checkIfFileIsEmpty(inputFile) || logs.split("\n")[0].equals(" ")) {
                log.info("File is empty! Please place log traces to read. Aborting. File read at: " + inputFile);
                return "";
            }
        } else {
            logs = inputFile;
            log.info("Processing logs from standard input..");
            log.info(logs);
        }
        return logs;
    }

    private void output(boolean usingFilesForIO, long executionTimeInSecs, int orphanLineCount
            , double averageDepth, double averageSize, String outputFileName, String jsonString, String inputFile) {

        StringBuilder stats = new StringBuilder();
        stats.append("\n<<Stats>>");
        stats.append("\nExecution Time(milliseconds):" + executionTimeInSecs);
        stats.append("\nOrphan lines:" + orphanLineCount);
        stats.append("\nAverage Depth:" + averageDepth);
        stats.append("\nAverage Size (char):" + averageSize);

        if (usingFilesForIO) {
            if (!writeToFile("stats.txt", stats.toString())) return;
            if (!writeToFile(outputFileName, jsonString)) return;
            log.info("<<<<<<<<<<Success! Sent output to the following Files>>>>>>>>>>>>");
            log.info("<Read logs successfully from File: " + inputFile + ">");
            log.info("Statistics written to: <stats.txt>");
            log.info("Output written to: <" + outputFileName + ">");

        } else {
            log.info("<<<<<<<<<<Success! Printing Output Tree>>>>>>>>>>>>");
            log.info(jsonString);
            log.info(stats.toString());
        }
    }


    private LogEntry populateLogEntryFromLine(String line, int count) {
        String[] spanSplit = line.split("->");
        String[] spaceSplit = spanSplit[0].split(" |T");
        LogEntry logEntry = new LogEntry();

        try {
            String span = spanSplit[1];
            String startTime = spaceSplit[0] + " " + spaceSplit[1];
            String endTime = spaceSplit[2] + " " + spaceSplit[3];
            String traceId = spaceSplit[4];
            String serviceName = spaceSplit[5];
            String callerSpan = spaceSplit[6];

            logEntry.setTraceId(traceId);
            logEntry.setCallerSpan(callerSpan);
            logEntry.setServiceName(serviceName);

            logEntry.setSpan(span);
            logEntry.setStartTime(startTime);
            logEntry.setEndTime(endTime);
        } catch (Exception e) {
            log.error("Ignored faulty log entry at Line: " + count, e);
        }
        return logEntry;
    }


    void parseLogFromStandardInput(String inputFromUser) throws IOException {
        parseLog(inputFromUser, null, false);
    }

    private boolean checkIfFileIsEmpty(String inputFile) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            if (br.readLine() == null) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    private String getPrettyJSON(JSONObject traceObject) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(traceObject);
        } catch (Exception e) {
            log.error("Exception while using Gson to make JSON formatting Pretty", e);
        }
        return null;
    }

    @Deprecated
    void exploreCall() {
        Map<String, List<LogEntry>> logMap = new HashMap<>();
        for (LogEntry e : logMap.get("root")) {
            for (LogEntry e1 : e.getCalls()) {

            }
        }

        for (String span : logMap.keySet()) {
            for (LogEntry e1 : logMap.get(span)) {
                e1.toString();
            }
        }
    }

    Map<String, List<LogEntry>> getLogMap(List<LogEntry> logEntries) {
        Map<String, List<LogEntry>> logMap = new HashMap<>();
        for (LogEntry e : logEntries) {
            e.setCalls(findLogEntriesWithCallerSpan(logEntries, e.getSpan()));
        }

        try {
            log.info(new ObjectMapper().writeValueAsString(logMap));
        } catch (JsonProcessingException e1) {
            log.error("JSON Processing Failed", e1);
        }
        return logMap;
    }

    boolean writeToFile(String outputFileName, String output) {
        outputFileName = (outputFileName.length() > 0) ? outputFileName : "output.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            writer.write(output);
            return true;
        } catch (IOException e) {
            log.info("Cannot write to file. Maybe file doesnot exist" + outputFileName);
            return false;
        }
    }

    private List<LogEntry> findLogEntriesWithCallerSpan(List<LogEntry> logEntries, String callerSpan) {
        List<LogEntry> filteredLogEntries = new ArrayList<>();
        for (LogEntry e : logEntries) {
            if (e.getCallerSpan().equalsIgnoreCase(callerSpan))
                filteredLogEntries.add(e);
        }
        return filteredLogEntries;
    }

    private LogEntry findNullRootLogEntry(List<LogEntry> logEntries, String traceId) {
        for (LogEntry e : logEntries) {
            if (e.getTraceId().contains(traceId) && e.getCallerSpan().contains("null")) {
                return e;
            }
        }
        return null;
    }

    private List<String> getUniqueTraceIds(List<LogEntry> logEntries) {
        List<String> traceIds = new ArrayList<>();
        for (LogEntry e : logEntries) {
            if (!traceIds.contains(e.getTraceId())) {
                traceIds.add(e.getTraceId());
            }
        }
        return traceIds;
    }

    private String readLogsFromFile(String inputFile) throws FileNotFoundException {
        StringBuilder stringBuilder = new StringBuilder();
        String fileName = (inputFile.length() > 0) ? inputFile : "logs.txt";
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        try (BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line + "\n"); // process the line
            }
        } catch (IOException e) {
            log.error("Failed IO at readLogsFromFile()", e);
        }

        return stringBuilder.toString();
    }
}
