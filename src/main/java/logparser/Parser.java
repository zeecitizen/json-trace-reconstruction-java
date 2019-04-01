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
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Parser {

    Parser() {
    }

    void parseLog(String inputFile, String outputFileName, Boolean usingFilesForIO) throws IOException {

        long executionStartTime = System.currentTimeMillis();
        String logs = "";
        if (usingFilesForIO) {
            logs = readLogsFromFile(inputFile);
            if (checkIfFileIsEmpty(inputFile) || logs.split("\n")[0].equals(" ")) {
                System.out.println("File is empty! Please place log traces to read. Aborting. File read at: " + inputFile);
                return;
            }
        } else {
            logs = inputFile;
            System.out.println("Processing logs from standard input..");
            System.out.println(logs);
        }
        String[] logLines = logs.split("\n");

        List<LogEntry> logEntries = new ArrayList<LogEntry>();
        double averageSize = 0d;
        double averageDepth = 0d;
        double numberOfOrphanTraces = 0d;

        double logLineLengthSum = 0;
        double logDepthSum = 0;
        int count = 0;
        for (String line : logLines) {
            count++;
            logLineLengthSum += line.length();

            LogEntry logEntry = populateLogEntryFromLine(line, count);

            logEntries.add(logEntry);
        }

        for (LogEntry e : logEntries)
        {
            e.setCalls(findLogEntriesWithCallerSpan(logEntries, e.getSpan()));
        }

        for (LogEntry e : logEntries)
        {
            logDepthSum += e.getCalls().size();
        }

        List<String> traceIds = getUniqueTraceIds(logEntries);
        System.out.println("List of found trace ids: " + traceIds.toString());
        String jsonString = "";
        String emptyArrayJson = ",\n\\s{2,}\"calls\": \\[\\]\n";

        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
        builder.setPrettyPrinting();

        Gson gson = builder.create();

        for (String traceId : traceIds)
        {
            LogEntry rootEntry = findNullRootLogEntry(logEntries, traceId);

            if (rootEntry == null)
                continue; //ignore orphans

            jsonString += gson.toJson(rootEntry).replaceAll(emptyArrayJson, "\n").replaceFirst("\\{\n", "{\"id\": \"" + traceId + "\",\n\"root\": {\n") + "}\n";
        }
        System.out.println(jsonString);

        int orphanLineCount = 0;
        for (int i = 0; i < logEntries.size(); i++)
        {
            if (!jsonString.contains(logEntries.get(i).getServiceName())) {
                orphanLineCount++;
            }
        }
        averageDepth = logDepthSum / logEntries.size();
        averageSize = logLineLengthSum / logEntries.size();

        long executionEndTime = System.currentTimeMillis();

        long executionTimeInSecs = executionEndTime - executionStartTime;

        output(usingFilesForIO, executionTimeInSecs, orphanLineCount, averageDepth, averageSize, outputFileName, jsonString, inputFile);
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
            if (writeToFile("stats.txt", stats.toString()) == false) return;
            if (writeToFile(outputFileName, jsonString) == false) return;
            System.out.println("<<<<<<<<<<Success! Sent output to the following Files>>>>>>>>>>>>");
            System.out.println("<Read logs successfully from File: " + inputFile + ">");
            System.out.println("Statistics written to: <stats.txt>");
            System.out.println("Output written to: <" + outputFileName + ">");

        } else {
            System.out.println("<<<<<<<<<<Success! Printing Output Tree>>>>>>>>>>>>");
            System.out.println(jsonString);
            System.out.println(stats.toString());
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
            System.out.println("Ignored faulty log entry at Line: " + count);
            e.printStackTrace();
        }

        return logEntry;
    }


    void parseLogFromStandardInput(String inputFromUser) throws IOException {
        parseLog(inputFromUser, null, false);
    }

    private boolean checkIfFileIsEmpty(String inputFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        if (br.readLine() == null) {
            br.close();
            return true;
        }
        br.close();
        return false;
    }

    private String getPrettyJSON(JSONObject traceObject) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(traceObject);
        } catch (Exception e) {
            System.out.println("Exception while using Gson to make JSON formatting Pretty");
            e.printStackTrace();
        }
        return null;
    }

    LogEntry findLogEntry(List<LogEntry> logEntries) {
        return new LogEntry();
    }

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
            System.out.println(new ObjectMapper().writeValueAsString(logMap));
        } catch (JsonProcessingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return logMap;
    }

    boolean writeToFile(String outputFileName, String output) {
        outputFileName = (outputFileName.length() > 0) ? outputFileName : "output.txt";
        BufferedWriter writer;
        try {

            writer = new BufferedWriter(new FileWriter(outputFileName));
            writer.write(output);
            writer.close();
            return true;
        } catch (IOException e) {
            System.out.println("Cannot write to file. Maybe file doesnot exist" + outputFileName);
            return false;
        }

    }

    List<LogEntry> findLogEntriesWithCallerSpan(List<LogEntry> logEntries, String callerSpan) {
        List<LogEntry> filteredLogEntries = new ArrayList<LogEntry>();
        for (LogEntry e : logEntries) {
            if (e.getCallerSpan().equalsIgnoreCase(callerSpan))
                filteredLogEntries.add(e);
        }

        return filteredLogEntries;
    }

    LogEntry findNullRootLogEntry(List<LogEntry> logEntries, String traceId) {

        for (LogEntry e : logEntries) {
            if (e.getTraceId().contains(traceId) && e.getCallerSpan().contains("null")) {
                return e;
            }
        }
        return null;
    }

    List<String> getUniqueTraceIds(List<LogEntry> logEntries) {

        List<String> traceIds = new ArrayList<String>();
        for (LogEntry e : logEntries) {
            if (!traceIds.contains(e.getTraceId())) {
                traceIds.add(e.getTraceId());
            }
        }
        return traceIds;
    }

    String readLogsFromFile(String inputFile) {

        StringBuilder stringBuilder = new StringBuilder();

        String fileName = (inputFile.length() > 0) ? inputFile : "logs.txt";

        try {
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                // process the line
                stringBuilder.append(line + "\n");
                // System.out.println(line);
            }

            br.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return stringBuilder.toString();
    }
}
