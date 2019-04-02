package logparser;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.SerializedName;

public class LogEntry implements Comparable<LogEntry> {

    @SerializedName("id")
    transient String traceId;

    @SerializedName("service")
    String serviceName;

    @SerializedName("start")
    String startTime;

    @SerializedName("end")
    String endTime;

    @SerializedName("calls")
    List<LogEntry> calls;

    @JsonIgnore
    private transient HashMap<String, LogEntry> map = new HashMap<String, LogEntry>();

    @JsonIgnore
    private transient String callerSpan;

    @JsonIgnore
    private transient String span;

    private String getName() {
        return traceId + span;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public List<LogEntry> getCalls() {
        return calls;
    }

    public void setCalls(List<LogEntry> calls) {
        this.calls = calls;
    }

    @JsonIgnore
    public String getCallerSpan() {
        return callerSpan;
    }

    @JsonIgnore
    public void setCallerSpan(String callerSpan) {
        this.callerSpan = callerSpan;
    }

    @JsonIgnore
    public String getSpan() {
        return span;
    }

    @JsonIgnore
    public void setSpan(String span) {
        this.span = span;
    }

    @JsonIgnore
    public String getTraceId() {
        return traceId;
    }

    @JsonIgnore
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @JsonIgnore
    public HashMap<String, LogEntry> getMap() {
        return map;
    }

    @JsonIgnore
    public void setMap(HashMap<String, LogEntry> map) {
        this.map = map;
    }

    @Deprecated
    public void addNeighbor(LogEntry node) {
        if (!map.containsKey(node.getName())) {
            if (!calls.contains(node))
                calls.add(node);
            map.put(node.getName(), node);
        }
    }

    @Override
    public int compareTo(LogEntry x) {
        //TODO: Must implement this to facilitate sorting of trace logs when producing JSON Output.
        return 0;
    }
}