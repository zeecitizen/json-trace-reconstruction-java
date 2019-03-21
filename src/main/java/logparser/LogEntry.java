package logparser;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LogEntry {
	
	String startTime;
	String endTime;
	String serviceName;
	
	List<LogEntry>calls;
	
	@JsonIgnore
	String callerSpan;
	@JsonIgnore
	String span;
	@JsonIgnore
	String traceId;

	LogEntry() {

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

}