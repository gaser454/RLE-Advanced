// rle.java
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class rle {
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String RED = "\u001B[91m";
    private static final String YELLOW = "\u001B[93m";

    private static String colorize(String text, String color) {
        return color + text + RESET;
    }

    private static final char ESCAPE = '\\';

    private static String compress(String text) {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char ch = text.charAt(i);
            int j = i + 1;
            while (j < n && text.charAt(j) == ch) j++;
            int count = j - i;
            if (count >= 3) {
                result.append(ch);
                result.append(ESCAPE);
                result.append(count);
            } else {
                for (int k = 0; k < count; k++) {
                    if (ch == ESCAPE) result.append(ESCAPE);
                    result.append(ch);
                }
            }
            i = j;
        }
        return result.toString();
    }

    private static String decompress(String text) throws Exception {
        if (text == null || text.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char ch = text.charAt(i);
            if (ch == ESCAPE) {
                if (i + 1 < n && text.charAt(i + 1) == ESCAPE) {
                    result.append(ESCAPE);
                    i += 2;
                    continue;
                }
                if (i + 1 >= n) throw new Exception("Unexpected end after escape");
                char repeatChar = text.charAt(i + 1);
                i += 2;
                StringBuilder numStr = new StringBuilder();
                while (i < n && Character.isDigit(text.charAt(i))) {
                    numStr.append(text.charAt(i));
                    i++;
                }
                if (numStr.length() == 0) throw new Exception("Missing number after escape");
                int count = Integer.parseInt(numStr.toString());
                for (int k = 0; k < count; k++) result.append(repeatChar);
            } else {
                result.append(ch);
                i++;
            }
        }
        return result.toString();
    }

    private static String readInput(String filename) throws IOException {
        if (filename == null || filename.equals("-")) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
        return new String(Files.readAllBytes(Paths.get(filename)), StandardCharsets.UTF_8);
    }

    private static void writeOutput(String filename, String content) throws IOException {
        if (filename == null || filename.equals("-")) {
            System.out.print(content);
        } else {
            Files.write(Paths.get(filename), content.getBytes(StandardCharsets.UTF_8));
        }
    }

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        boolean verbose = false;
        String mode = null, inputFile = null, outputFile = null;
        for (String arg : argList) {
            if (arg.equals("-v") || arg.equals("--verbose")) {
                verbose = true;
            } else if (mode == null && (arg.equals("compress") || arg.equals("decompress"))) {
                mode = arg;
            } else if (inputFile == null) {
                inputFile = arg;
            } else {
                outputFile = arg;
            }
        }

        if (mode == null) {
            System.out.println(colorize("Usage: java rle compress|decompress [options] <input> [output]", YELLOW));
            System.out.println("Options: -v, --verbose  show statistics");
            return;
        }

        String data;
        try {
            data = readInput(inputFile);
        } catch (Exception e) {
            System.err.println(colorize("Error reading input: " + e.getMessage(), RED));
            return;
        }
        int origSize = data.getBytes(StandardCharsets.UTF_8).length;

        String result;
        try {
            if (mode.equals("compress")) result = compress(data);
            else result = decompress(data);
        } catch (Exception e) {
            System.err.println(colorize("Error: " + e.getMessage(), RED));
            return;
        }
        int compSize = result.getBytes(StandardCharsets.UTF_8).length;

        if (verbose && mode.equals("compress")) {
            double ratio = origSize > 0 ? (double) compSize / origSize : 1.0;
            double saving = (1.0 - ratio) * 100.0;
            System.out.println(colorize("Original size: " + origSize + " bytes", YELLOW));
            System.out.println(colorize("Compressed size: " + compSize + " bytes", YELLOW));
            System.out.println(colorize(String.format("Ratio: %.2fx", ratio), GREEN));
            System.out.println(colorize(String.format("Space saving: %.1f%%", saving), GREEN));
        }

        try {
            writeOutput(outputFile, result);
            if (outputFile != null && !outputFile.equals("-")) {
                System.out.println(colorize("Result written to " + outputFile, GREEN));
            }
        } catch (Exception e) {
            System.err.println(colorize("Error writing output: " + e.getMessage(), RED));
        }
    }
}
