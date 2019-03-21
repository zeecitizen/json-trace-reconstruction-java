# json-trace-reconstruction-java
A Java program that produces pretty printed JSON trees from given log traces. 

## Running the application
Run Main.java from package logparser and it will convert the logs placed in logs.txt to pretty printed JSON and print on console as well as save it to output.txt. The statistics are sent to stats.txt


## Config
- the program takes two args
- argument one: Path to file logs.txt where we can read the Logs as input to the program
- argument two: Path to output.txt where we can output the JSON

## Ouputs
- output.txt, we print the JSON and output to this file
- statistics.txt, we print a summary of statistics related to the trace logs


## Introduction
- Each application in a microservice environment outputs some log describing the boundaries of an HTTP request, with the following format:

[start-timestamp] [end-timestamp] [trace] [service-name] [caller-span]->[span]

The trace ID is a random string that is passed along every service interaction. The first service (called from outside) generates the string and passes it to every other service it calls during the execution of the request. The called services take the trace (let’s say, from an HTTP header) and also pass it to the services the call themselves.

The span ID is generated for every request. When a service calls another, it passes its own span ID to the callee. The callee will generate its own span ID, and using the span passed by the caller, log the last part of the line, that allows to connect the requests.

So, a trace could look like this:

2016-10-20 12:43:34.000 2016-10-20 12:43:35.000 trace1 back-end-3 ac->ad
2016-10-20 12:43:33.000 2016-10-20 12:43:36.000 trace1 back-end-1 aa->ac
2016-10-20 12:43:38.000 2016-10-20 12:43:40.000 trace1 back-end-2 aa->ab
2016-10-20 12:43:32.000 2016-10-20 12:43:42.000 trace1 front-end null->aa

Meaning that the “front-end” received a call from the outside (“null”), and assigned the span “aa” to the request. Then it called back-end-1, who assigned the span “ac”, who in turn called service “back-end-3”, who assigned span “ad”. Then, “front-end” called “back-end-2”.

The entries are logged when the request finishes (as they contain the finishing time), so they are not in calling order, but in finishing order. 

Logs can be mixed up a bit (just because enforcing FIFO semantics is hard in distributed setups), but it is expected that the vast majority are only off for a few milliseconds. 

Timestamps are in UTC.

This execution trace can then be represented as:

```json
{“id: “trace1”,
“root”: {
       “service”: “front-end”,
        “start”: “2016-10-20 12:43:32.000”,
        “end”: “2016-10-20 12:43:42.000”,
        “calls”: [
             {“service”: “back-end-1”,
              “start”: “2016-10-20 12:43:33.000”,
              “end”: “2016-10-20 12:43:36.000”,
              “calls”: [
                    {“service”: “back-end-3”,
                     “start”: “2016-10-20 12:43:34.000”,
                     “end”: “2016-10-20 12:43:35.000”}]},
              {“service”, “back-end-2”,
               “start”: “2016-10-20 12:43:38.000”,
               “end”: “2016-10-20 12:43:40.000”}
]}}
```

The task is to produce these JSON trees. That is, given a sequence of log entries, output a JSON for each trace. We should imagine that this application could be deployed as part of some pipeline that starts at the source of the data and ends in some other monitoring application, that presents a stream of recent traces.


## Detailed Requirements:
-	The solution should be a Java program, executable from the command line.
-	The input should be read from standard input or a file (chooseable by the user)
-	The output should be one JSON per line, written to standard output, or a file (chooseable by the user).
-	As said, there can be lines out of order.
-	There can be orphan lines (i.e., services with no corresponding root service); they should be tolerated but not included in the output (maybe summarized in stats, see below).
-	Lines can be malformed, they should be tolerated and ignored.

## Features:
-	A nice command-line interface, that allows to specify inputs and outputs from and to files.
-	Optionally report to standard error (or to a file), statistics about progress, lines consumed, line consumption rate, buffers, etc. Both at the end of the processing and during it.
-	Optionally report to standard error (or to a file), statistics about the traces themselves, like number of orphan requests, average size of traces, average depth, etc.
-	As the file could be quite big, try to do the processing using as many cores as the computer has, but only if the processing is actually speeded that way.
