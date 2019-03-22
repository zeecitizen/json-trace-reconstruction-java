package logparser;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {

        // Create a scanner to wrap the standard input stream
        Scanner scanner = new Scanner(System.in);
        String response = "";
        do {
            // Prompt user to enter a string and press enter
            printWelcome();
            response = scanner.nextLine();
            System.out.println("You Entered: " + response);
        } while ((response.trim().equals("F") || response.trim().equals("S")) == false);

        while ((response.trim().equals("F") || response.trim().equals("S")) == true) {

            if (response.equals("F")) {
                do {
                    System.out.println("Please select an option: ");
                    System.out.println("Input Key 'A' - Automatically create files on current PATH for input/output.");
                    System.out.println("Input Key 'M' - I'll input the path manually");
                    response = scanner.nextLine();
                    System.out.println("You Entered: " + response);
                } while ((response.trim().equals("A") || response.trim().equals("M")) == false);

                if (response.trim().equals("A")) {
                    try {
                        String[] filePaths = getFilePaths();
                        System.out.println("File One Created was: " + filePaths[0]);
                        System.out.println("File Two Created was: " + filePaths[1]);
                        new Parser().parseLog(filePaths[0], filePaths[1], true);
                    } catch (Exception e) {
                        System.out.println("Cannot automatically find files. Try re-running and pressing M for manually entering file names.");
                        e.printStackTrace();
                    }
                    break;
                } else if (response.trim().equals("M")) {
                    String[] paths = null;
                    do {
                        System.out.println("We need two file paths to work with. The input is taken from logs.txt and output from output.txt");
                        System.out.println("Please enter two paths separated by commas. (strictly path/logs.txt, path/output.txt");
                        response = scanner.nextLine();
                        System.out.println("You Entered: " + response);
                        paths = response.split(",");
                        if (!filesExist(paths)) return;
                    } while ((paths[0].trim().endsWith(".txt") || paths[1].trim().endsWith(".txt")) == false);

                    try {
                        new Parser().parseLog(paths[0], paths[1], true);
                    } catch (Exception e) {
                        System.out.println("Something went wrong while parsing from given paths. Please check file paths");
                        System.out.println("File One Specified was: " + paths[0]);
                        System.out.println("File Two Specified was: " + paths[1]);
                        e.printStackTrace();
                    }
                    break;
                }
            }

            if (response.equals("S")) {
                System.out.println("Please paste input logs here and press enter to process: ");
                Scanner stdin = new Scanner(new BufferedInputStream(System.in));
                String token = null;
                response = "";
                while ((token = stdin.nextLine()).length() > 0) {
                    response = response + token + "\n";
                }

                System.out.println("You Entered: " + response);
                try {
                    new Parser().parseLogFromStandardInput(response);
                } catch (Exception e) {
                    System.out.println("Something went wrong while parsing from given standard input. Please check input");
                    System.out.println("The standard input was: " + response);
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private static boolean filesExist(String[] paths) throws IOException {

        int count = 0;
        for (String path : paths) {
            File f = new File(paths[count++]);

            if (f.exists() && !f.isDirectory()) {
                System.out.println("File found " + f.getAbsolutePath());
            } else {
                System.out.println("Sorry! File not found " + f.getAbsolutePath());
                System.out.println("Creating this file for you.");
                try {
                    f.createNewFile();
                    if (f.exists() && !f.isDirectory()) {
                        System.out.println("Created file successfully. Run program again.");
                        System.out.println("File created at " + f.getAbsolutePath());
                    } else {
                        System.out.println("System cannot create this file for you. Run program again after creating text file at " + f.getAbsolutePath());
                    }

                } catch (IOException e) {
                    //e.printStackTrace();
                    System.out.println("System cannot create this file for you. Run program again after creating text file at " + f.getAbsolutePath());
                    return false;
                }
            }
        }
        return true;
    }

    public static void printWelcome() {
        System.out.println("Please select a choice: ");
        System.out.println("Input Key 'F' - I will specify input, output files PATH.");
        System.out.println("Input Key 'S' - Don't use files, just get standard input here and output here on console");
    }

    public static String[] getFilePaths() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("logs.txt", "UTF-8");
        writer.println("2016-10-20 12:43:34.000 2016-10-20 12:43:35.000 trace1 back-end-3 ac->ad");
        writer.println("2016-10-20 12:43:33.000 2016-10-20 12:43:36.000 trace1 back-end-1 aa->ac");
        writer.println("2016-10-20 12:43:38.000 2016-10-20 12:43:40.000 trace1 back-end-2 aa->ab");
        writer.println("2016-10-20 12:43:32.000 2016-10-20 12:43:42.000 trace1 front-end null->aa");
        writer.close();

        PrintWriter writer2 = new PrintWriter("output.txt", "UTF-8");
        writer.println(" ");
        writer.close();

        String[] files = new String[2];
        files[0] = "logs.txt";
        files[1] = "output.txt";
        return files;
    }
}
