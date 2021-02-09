package me.XXX.eesearcher.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final Pattern SPACE = Pattern.compile("[\\s]");

    public static boolean isValidPDF(final File file) {
        try (final Scanner scanner = new Scanner(new FileInputStream(file))) {
            final String s = scanner.nextLine();
            return s.startsWith("%PDF");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean isValidPDF(final Path path) {
        return isValidPDF(path.toFile());
    }

    public static String titleCase(final String s) {
        final Matcher matcher = SPACE.matcher(s);
        final StringJoiner joiner = new StringJoiner(" ");
        while (matcher.find()) {
            final char[] arr = matcher.group().toCharArray();
            arr[0] = Character.toUpperCase(arr[0]);
            joiner.add(new String(arr));
        }
        return joiner.toString();
    }

}
