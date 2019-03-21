package logparser;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.Iterator;

public class Main {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                String pathToSearch = "./Logging-master/";
                new Parser().parseLog(getFilePath(pathToSearch,"logs"), getFilePath(pathToSearch,"output"));
            } else
                new Parser().parseLog(args[0], args[1]);
        } catch (Exception e) {
            System.out.println("Please check that you've provided correct file names as parameters");
            System.out.println("We need file path to logs.txt and output.txt provided as args when executing program.");
            e.printStackTrace();
        }
    }

    public static String getFilePath(String pathToSearch, String fileNameWithoutExtension) {
        File dir = new File(pathToSearch);
        File[] matches = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(fileNameWithoutExtension) && name.endsWith(".txt");
            }
        });
        return matches[0].getAbsolutePath();
    }
}
