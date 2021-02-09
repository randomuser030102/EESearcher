package me.XXX.eesearcher;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * System configuration for the "server". All methods in this class are NOT
 * thread safe.
 */
public class SystemConfiguration {

    // %dir%
    public static final Pattern PLACEHOLDER_PATH_REGEX = Pattern.compile("(%dir%)");
    public static final String USER_KEY = "db.user";
    public static final String PASS_KEY = "db.pass";
    public static final String PATH_KEY = "db.path";
    public static final String IO_KEY = "io.max-threads";

    private final Properties properties = new Properties();
    private String databaseUsername = "username";
    private String databasePassword = "password";
    private File databasePath = new File(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getFile()).getParent(), "data.db");
    private int maxIO = -1;

    public SystemConfiguration() {
    }

    public SystemConfiguration(@NotNull final SystemConfiguration other) {
        this.databaseUsername = other.databaseUsername;
        this.databasePassword = other.databasePassword;
        this.databasePath = other.databasePath;
        this.maxIO = other.maxIO;
        updateProperties();
    }

    public SystemConfiguration(@NotNull final File file) throws IOException {
        final Properties properties = new Properties();
        try (final FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        }
        final String raw = properties.getProperty(PATH_KEY, "%dir%" + File.separator + "data.db");
        final String[] split = PLACEHOLDER_PATH_REGEX.split(raw);
        if (split.length < 2) {
            this.databasePath = Paths.get(raw).toFile();
        } else {
            this.databasePath = Paths.get("", split[1]).toFile();
        }
        this.databaseUsername = properties.getProperty(USER_KEY, this.databaseUsername);
        this.databasePassword = properties.getProperty(PASS_KEY, this.databaseUsername);
        this.maxIO = Integer.parseInt(properties.getProperty(IO_KEY, String.valueOf(maxIO)));
        if (this.maxIO == 0 || this.maxIO < -1) {
            throw new IOException(String.format("Invalid configuration detected! Max IO is invalid: %d", maxIO));
        }
    }

    private void updateProperties() {
        properties.setProperty(USER_KEY, databaseUsername);
        properties.setProperty(PASS_KEY, databasePassword);
        final String currentDir = Paths.get("").toFile().getAbsolutePath();
        final String currentDatabasePath = databasePath.getAbsolutePath();
        properties.setProperty(PATH_KEY, currentDatabasePath.replace(currentDir, "%dir%"));
        properties.setProperty(IO_KEY, String.valueOf(maxIO));
    }

    public void save() throws IOException {
        updateProperties();
        try (final FileOutputStream fis = new FileOutputStream(databasePath)) {
            // FIXME add comments
            properties.store(fis, "");
        }
    }

    public void setMaxIO(int maxIO) throws IllegalArgumentException {
        if (maxIO < -1 || maxIO == 0) {
            throw new IllegalArgumentException("Invalid MaxIO!");
        }
        this.maxIO = maxIO;
    }

    public void setDatabasePassword(@NotNull final String databasePassword) {
        this.databasePassword = Objects.requireNonNull(databasePassword);
    }

    public void setDatabasePath(@NotNull final File databasePath) {
        this.databasePath = Objects.requireNonNull(databasePath);
    }

    public void setDatabaseUsername(@NotNull final String databaseUsername) {
        this.databaseUsername = Objects.requireNonNull(databaseUsername);
    }

    public @NotNull String databaseUsername() {
        return this.databaseUsername;
    }

    public @NotNull String databasePassword() {
        return this.databasePassword;
    }

    public int maxIOThreads() {
        return this.maxIO;
    }

    public @NotNull File databasePath() {
        return this.databasePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SystemConfiguration that = (SystemConfiguration) o;

        if (maxIO != that.maxIO) return false;
        if (!databaseUsername.equals(that.databaseUsername)) return false;
        if (!databasePassword.equals(that.databasePassword)) return false;
        return databasePath.equals(that.databasePath);
    }

    @Override
    public int hashCode() {
        int result = databaseUsername.hashCode();
        result = 31 * result + databasePassword.hashCode();
        result = 31 * result + databasePath.hashCode();
        result = 31 * result + maxIO;
        return result;
    }
}
