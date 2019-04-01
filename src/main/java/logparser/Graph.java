package logparser;

import java.util.ArrayList;
import java.util.HashMap;

@Deprecated
public class Graph {
    private ArrayList<LogEntry> nodes = new ArrayList<LogEntry>();
    private HashMap<String, LogEntry> map = new HashMap<String, LogEntry>();

    public LogEntry getOrCreateNode(String calleeSpanID) {
        if (!map.containsKey(calleeSpanID)) {
            LogEntry node = new LogEntry();
            nodes.add(node);
            map.put(calleeSpanID, node);
        }

        return map.get(calleeSpanID);
    }

    public void addEdge(String startName, String endName) {
        LogEntry start = getOrCreateNode(startName);
        LogEntry end = getOrCreateNode(endName);
        start.addNeighbor(end);
    }

    public ArrayList<LogEntry> getNodes() {
        return nodes;
    }
}