package logparser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Parser {

    Parser() {
    }

    void parseLog(String inputFile, String outputFileName) {

        long executionStartTime = System.currentTimeMillis();
        String logs = readLogsFromFile(inputFile);
        String[] logLines = logs.split("\n");
        List<LogEntry> logEntries = new ArrayList<LogEntry>();
        double averageSize = 0d;
        double averageDepth = 0d;
        double numberOfOrphanTraces = 0d;

        double logLineLengthSum = 0;
        double logDepthSum = 0;

        for (String line : logLines) {
            logLineLengthSum += line.length();
            String[] spanSplit = line.split("->");
            String[] spaceSplit = spanSplit[0].split(" ");
            String span = spanSplit[1];
            String startTime = spaceSplit[0] + " " + spaceSplit[1];
            String endTime = spaceSplit[2] + " " + spaceSplit[3];
            String traceId = spaceSplit[4];
            String serviceName = spaceSplit[5];
            String callerSpan = spaceSplit[6];

            LogEntry logEntry = new LogEntry();
            logEntry.setTraceId(traceId);
            logEntry.setCallerSpan(callerSpan);
            logEntry.setServiceName(serviceName);

            logEntry.setSpan(span);
            logEntry.setStartTime(startTime);
            logEntry.setEndTime(endTime);

            logEntries.add(logEntry);
        }
        for (LogEntry e : logEntries) {

            e.setCalls(findLogEntriesWithCallerSpan(logEntries, e.getSpan()));
        }
        for (LogEntry e : logEntries) {
            logDepthSum += e.getCalls().size();
        }


        List<String> traceIds = getUniqueTraceId(logEntries);
        JSONObject traceObject = new JSONObject();

        for (String traceId : traceIds) {

            LogEntry rootEntry = findRootLogEntry(logEntries);

            if (rootEntry == null)
                continue;

            traceObject.put("id", traceId);

            JSONObject rootObject = new JSONObject();
            try {
                rootObject = new JSONObject(new ObjectMapper().writeValueAsString(rootEntry));
            } catch (JSONException | JsonProcessingException e1) {
                System.out.println(e1.getMessage());
            }
            traceObject.put("root", rootObject);

        }
        System.out.println("<<<<<<<<<<Output Tree>>>>>>>>>>>>");
        System.out.println(getPrettyJSON(traceObject));

        int orphanLineCount = 0;
        for (int i = 0; i < logEntries.size(); i++) {
            if (!traceObject.toString().contains(logEntries.get(i).getServiceName())) {
                orphanLineCount++;
            }
        }
        averageDepth = logDepthSum / logEntries.size();
        averageSize = logLineLengthSum / logEntries.size();


        long executionEndTime = System.currentTimeMillis();

        long executionTimeInSecs = executionEndTime - executionStartTime;

        StringBuilder stats = new StringBuilder();
        stats.append("\n<<Stats>>");
        stats.append("\nExecution Time(milliseconds):" + executionTimeInSecs);
        stats.append("\nOrphan lines:" + orphanLineCount);
        stats.append("\nAverage Depth:" + averageDepth);
        stats.append("\nAverage Size (char):" + averageSize);

        System.out.println(stats.toString());
        writeToFile("stats.txt", stats.toString());

        writeToFile(outputFileName, getPrettyJSON(traceObject));
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

    void writeToFile(String outputFileName, String output) {
        outputFileName = (outputFileName.length() > 0) ? outputFileName : "output.txt";
        BufferedWriter writer;
        try {

            writer = new BufferedWriter(new FileWriter(outputFileName));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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

    LogEntry findRootLogEntry(List<LogEntry> logEntries) {

        for (LogEntry e : logEntries) {
            if (e.getCallerSpan().contains("null")) {
                return e;
            }
        }
        return null;
    }

    List<String> getUniqueTraceId(List<LogEntry> logEntries) {

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
