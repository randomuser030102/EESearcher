package me.XXX.eesearcher.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents an IB Subject and can be serialized.
 * This class is immutable by nature and is therefore
 * thread-safe.
 */
public final class Subject implements Serializable {

    public static final Subject WORLD_STUDIES = new Subject(false, (byte) 0, "World Studies", true);

    private final boolean isActive;
    private final byte group;
    private final String displayName;
    private final HashSet<String> aliases;
    private int hash = Integer.MIN_VALUE;

    private Subject(final boolean validate, final byte group, final String displayName, boolean isActive, String... aliases) {
        if (validate) {
            switch (group) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    this.group = group;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid Group!");
            }
        } else {
            this.group = group;
        }
        this.isActive = isActive;
        this.displayName = displayName;
        if (aliases != null) {
            this.aliases = Arrays.stream(aliases)
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toCollection(HashSet::new));
        } else {
            this.aliases = new HashSet<>();
        }
    }

    public Subject(final byte group, final String displayName, boolean isActive, final String... aliases) {
        this(true, group, displayName, isActive, aliases);
    }

    public boolean isActive() {
        return isActive;
    }

    public byte getGroup() {
        return group;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public boolean isSubject(final String name) {
        if (name == null) {
            return false;
        }
        return this.displayName.equalsIgnoreCase(name) || this.aliases.contains(name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Subject{" +
                "isActive=" + isActive +
                ", group=" + group +
                ", displayName='" + displayName + '\'' +
                ", aliases=" + aliases +
                ", hash=" + hash +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subject subject = (Subject) o;

        if (isActive != subject.isActive) return false;
        if (group != subject.group) return false;
        if (hash != subject.hash) return false;
        if (!Objects.equals(displayName, subject.displayName)) return false;
        return Objects.equals(aliases, subject.aliases);
    }

    @Override
    public int hashCode() {
        if (this.hash == Integer.MIN_VALUE) {
            int result = (isActive ? 1 : 0);
            result = 31 * result + (int) group;
            result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
            result = 31 * result + (aliases != null ? aliases.hashCode() : 0);
            this.hash = result;
        }
        return this.hash;
    }
}
