package logparser;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Scanner;

public class Main {

    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);
        String response;


        do {
            printWelcome();
            response = scanner.nextLine();
            log(response);
        } while (!(response.trim().equals("F") || response.trim().equals("S")));

        while ((response.trim().equals("F") || response.trim().equals("S"))) {

            if (response.equals("F")) {
                do {
                    log.info("Please select an option: ");
                    log.info("Input Key 'A' - Automatically create files on current PATH for input/output.");
                    log.info("Input Key 'M' - I'll input the path manually");
                    response = scanner.nextLine();
                    log(response);
                } while (!(response.trim().equals("A") || response.trim().equals("M")));

                if (response.trim().equals("A")) {
                    try {
                        String[] filePaths = getFilePaths();
                        log.info("File One Created was: " + filePaths[0]);
                        log.info("File Two Created was: " + filePaths[1]);
                        new Parser().parseLog(filePaths[0], filePaths[1], true);
                    } catch (Exception e) {
                        log.error("Cannot automatically find files. Try re-running and pressing M for manually entering file names.", e);
                    }
                    break;
                } else if (response.trim().equals("M")) {
                    String[] paths = null;
                    do {
                        log.info("We need two file paths to work with. The input is taken from logs.txt and output from output.txt");
                        log.info("Please enter two paths separated by commas. (strictly path/logs.txt, path/output.txt");
                        response = scanner.nextLine();
                        log(response);
                        paths = response.split(",");
                        if (!filesExist(paths)) return;
                    } while (!(paths[0].trim().endsWith(".txt") || paths[1].trim().endsWith(".txt")));

                    try {
                        new Parser().parseLog(paths[0], paths[1], true);
                    } catch (Exception e) {
                        log.error("Something went wrong while parsing from given paths. Please check file paths", e);
                        log.info("File One Specified was: " + paths[0]);
                        log.info("File Two Specified was: " + paths[1]);
                    }
                    break;
                }
            }

            if (response.equals("S")) {
                log.info("Please paste input logs here and press enter to process: ");
                Scanner stdin = new Scanner(new BufferedInputStream(System.in));
                String token = null;
                response = "";
                while ((token = stdin.nextLine()).length() > 0) {
                    response = response + token + "\n";
                }

                log(response);
                try {
                    new Parser().parseLogFromStandardInput(response);
                } catch (Exception e) {
                    log.error("Something went wrong while parsing from given standard input. Please check input", e);
                    log.info("The standard input was: " + response);
                }
                break;
            }
        }
    }

    private static void log(String response) {
        log.info("You Entered: " + response);
    }

    private static boolean filesExist(String[] paths) throws IOException {

        int count = 0;
        for (String path : paths) {
            File f = new File(paths[count++]);

            if (f.exists() && !f.isDirectory()) {
                log.info("File found " + f.getAbsolutePath());
            } else {
                log.info("Sorry! File not found " + f.getAbsolutePath());
                log.info("Creating this file for you.");
                try {
                    f.createNewFile();
                    if (f.exists() && !f.isDirectory()) {
                        log.info("Created file successfully. Run program again.");
                        log.info("File created at " + f.getAbsolutePath());
                    } else {
                        log.info("System cannot create this file for you. Run program again after creating text file at " + f.getAbsolutePath());
                    }

                } catch (IOException e) {
                    log.info("System cannot create this file for you. Run program again after creating text file at " + f.getAbsolutePath());
                    return false;
                }
            }
        }
        return true;
    }

    private static void printWelcome() {
        log.info("Please select a choice: ");
        log.info("Input Key 'F' - I will specify input, output files PATH.");
        log.info("Input Key 'S' - Don't use files, just get standard input here and output here on console");
    }

    private static String[] getFilePaths() throws FileNotFoundException, UnsupportedEncodingException {

        try (PrintWriter writer = new PrintWriter("logs.txt", "UTF-8")) {
            writer.println("2016-10-20 12:43:34.000 2016-10-20 12:43:35.000 trace1 back-end-3 ac->ad");
            writer.println("2016-10-20 12:43:33.000 2016-10-20 12:43:36.000 trace1 back-end-1 aa->ac");
            writer.println("2016-10-20 12:43:38.000 2016-10-20 12:43:40.000 trace1 back-end-2 aa->ab");
            writer.println("2016-10-20 12:43:32.000 2016-10-20 12:43:42.000 trace1 front-end null->aa");
        }

        try (PrintWriter writer2 = new PrintWriter("output.txt", "UTF-8")) {
            writer2.println(" ");
        }

        String[] files = new String[2];
        files[0] = "logs.txt";
        files[1] = "output.txt";
        return files;
    }
}
