package me.XXX.eesearcher.data;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a cache of all {@link Subject} which are known.
 * This class is thread safe.
 */
public final class SubjectDatabase implements Serializable {

    // Non-fair sync as we expect much more reads than writes
    private final ReentrantReadWriteLock parentLock = new ReentrantReadWriteLock(false);

    private final Set<Subject> subjects = new HashSet<>();
    private final Set<Subject> activeSubjects = new HashSet<>();
    private final Set<Subject> inActiveSubjects = new HashSet<>();
    private final Map<String, Subject> displayNameMap = new HashMap<>();
    private final Map<Byte, Set<Subject>> groupSubjectMap = new HashMap<>();

    SubjectDatabase() {
        for (int i = 0; i < 7; i++) {
            groupSubjectMap.put((byte) i, new HashSet<>());
        }
        unsafeRegisterSubject(Subject.WORLD_STUDIES);
    }

    private static void validateGroup(final byte group) {
        switch (group) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                return;
            default:
                throw new IllegalArgumentException("Invalid Group: " + group);
        }
    }

    public @NotNull Optional<@NotNull Subject> getSubjectByName(final String name) {
        final Lock readLock = parentLock.readLock();
        try {
            readLock.lock();
            return Optional.ofNullable(displayNameMap.get(name)).or(() -> {
                for (Subject s : subjects) {
                    if (s.isSubject(name)) {
                        return Optional.of(s);
                    }
                }
                return Optional.empty();
            });
        } finally {
            readLock.unlock();
        }
    }

    public Set<Subject> getSubjectsByName(final String name) {
        final Lock readLock = parentLock.readLock();
        final Set<Subject> byName = new HashSet<>();
        try {
            readLock.lock();
            for (Subject s : subjects) {
                if (s.isSubject(name)) {
                    byName.add(s);
                }
            }
        } finally {
            readLock.unlock();
        }
        return byName;
    }

    public Set<Subject> getSubjectsByGroup(final byte group) {
        validateGroup(group);
        final Lock lock = parentLock.readLock();
        try {
            lock.lock();
            return new HashSet<>(groupSubjectMap.get(group));
        } finally {
            lock.unlock();
        }
    }

    public @NotNull List<@NotNull Subject> getActiveSubjects() {
        final Lock lock = parentLock.readLock();
        try {
            lock.lock();
            return new ArrayList<>(activeSubjects);
        } finally {
            lock.unlock();
        }
    }

    public @NotNull List<@NotNull Subject> getInActiveSubjects() {
        final Lock lock = parentLock.readLock();
        try {
            lock.lock();
            return new ArrayList<>(inActiveSubjects);
        } finally {
            lock.unlock();
        }
    }

    public boolean isSubject(final String name) {
        return getSubjectByName(name).isPresent();
    }

    public void registerSubject(final Subject subject) throws IllegalArgumentException {
        if (isSubject(subject.getDisplayName())) {
            throw new IllegalArgumentException("Invalid Subject: " + subject.getDisplayName());
        }
        final Lock writeLock = parentLock.writeLock();
        try {
            writeLock.lock();
            subjects.add(subject);
            displayNameMap.put(subject.getDisplayName(), subject);
            groupSubjectMap.get(subject.getGroup()).add(subject);
            if (subject.isActive()) {
                activeSubjects.add(subject);
            } else {
                inActiveSubjects.remove(subject);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void unsafeRegisterSubject(final Subject subject) {
        final Lock writeLock = parentLock.writeLock();
        try {
            writeLock.lock();
            subjects.add(subject);
            displayNameMap.put(subject.getDisplayName(), subject);
            groupSubjectMap.get(subject.getGroup()).add(subject);
            if (subject.isActive()) {
                activeSubjects.add(subject);
            } else {
                inActiveSubjects.remove(subject);
            }
        } finally {
            writeLock.unlock();
        }
    }


}
