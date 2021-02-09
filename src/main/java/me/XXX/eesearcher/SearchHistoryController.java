package me.XXX.eesearcher;

import com.google.inject.Singleton;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * Thread-Safe implementation of the SearchHistory.
 * All methods can be executed from multiple threads.
 */
@Singleton
public class SearchHistoryController {

    /**
     * Represents the actual search history terms
     */
    private final LinkedList<String> history = new LinkedList<>();

    /**
     * Mirrors {@link #history} in order to speed up lookup times to O(1)
     */
    private transient final Set<String> cache = new HashSet<>();

    /**
     * Max size of the history before values will be truncated
     */
    public volatile transient int HISTORY_MAX_SIZE = 30;

    /**
     * Save the history to a file
     *
     * @param file The {@link File} to save to
     * @throws IOException Thrown if there was an error writing the values to disk
     */
    public synchronized void save(final File file) throws IOException {
        final LinkedList<String> copy;
        synchronized (this.history) {
            copy = new LinkedList<>(this.history);
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream outputStream = new ObjectOutputStream(fos)) {
            outputStream.writeObject(copy);
        }
    }

    /**
     * Load the history from a file
     *
     * @param file      The {@link File} to read from
     * @param mergeData Whether the current history should be merged with the
     *                  history loaded from disk
     * @throws IOException Thrown if there is an error in loading the data from disk
     */
    @SuppressWarnings("unchecked")
    public synchronized void loadData(final File file, boolean mergeData) throws IOException {
        final LinkedList<String> serial;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream inputStream = new ObjectInputStream(fis)) {
            final Object obj = inputStream.readObject();
            if (!(obj instanceof LinkedList)) {
                // FIXME log invalid object.
                return;
            }
            final LinkedList<?> linkedList = (LinkedList<?>) obj;
            final Iterator<?> iterator = linkedList.iterator();
            while (iterator.hasNext()) {
                final Object o = iterator.next();
                if (o == null) {
                    // Gracefully handle null entries from deserialization
                    iterator.remove();
                    continue;
                }
                if (!(o instanceof String)) {
                    // FIXME log invalid list
                    return;
                }
            }
            // Potential performance improvement: pre-calculate how many elements should be added / will be truncated
            synchronized (this.history) {
                if (!mergeData) {
                    this.history.clear();
                    this.cache.clear();
                }
                this.history.addAll((LinkedList<String>) linkedList);
                this.cache.addAll(this.history);
            }
            truncateHistory();
        } catch (ClassNotFoundException ex) {
            // Should never happen since LinkedList is a part of stdlib; indicates data is corrupted
            // FIXME log CNFE
        }
    }

    /**
     * Get a copy of the currently search history. Changes to this list
     * will not be reflected in this controller and vice versa.
     *
     * @return Returns a {@link List} representing the search queries.
     */
    public @NotNull List<@NotNull String> getHistory() {
        synchronized (this.history) {
            return new ArrayList<>(this.history);
        }
    }

    /**
     * Append values to the search history
     *
     * @param history The history to append
     */
    public void offerHistory(@NotNull final List<@NotNull String> history) {
        synchronized (this.history) {
            this.history.clear();
            for (String s : history) {
                // Forcefully validate objects aren't null
                if (s == null) {
                    continue;
                }
                this.cache.add(s);
                this.history.add(s);
            }
        }
    }

    /**
     * Add an entry to the search history
     *
     * @param entry The string entry
     */
    public void addEntry(@NotNull final String entry) {
        synchronized (this.history) {
            final String added = Objects.requireNonNull(entry).toLowerCase(Locale.ROOT);
            this.history.add(added);
            this.cache.add(added);
        }
    }

    /**
     * Remove the first entry
     *
     * @return Returns an {@link Optional} populated with the removed value, empty
     * if the search history is empty.
     */
    public @NotNull Optional<@NotNull String> removeFirstEntry() {
        synchronized (this.history) {
            if (this.history.isEmpty()) {
                return Optional.empty();
            }
            final String removed = this.history.remove();
            this.cache.remove(removed);
            return Optional.of(this.history.remove());
        }
    }

    /**
     * Remove a specified entry from the search history
     *
     * @param entry The entry to remove.
     * @return Returns whether the search history changed as a result of the deletion
     */
    public boolean removeEntry(@NotNull String entry) {
        synchronized (this.history) {
            if (this.cache.remove(entry)) {
                return this.history.remove(entry);
            }
            return false;
        }
    }

    /**
     * Get the last entry from search history
     *
     * @return Returns an {@link Optional} which is populated by the search entry, empty
     * if the search history is empty
     */
    public @NotNull Optional<@NotNull String> lastEntry() {
        synchronized (this.history) {
            if (this.history.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(this.history.getLast());
        }
    }

    /**
     * Check whether a specified entry exists within the search history
     *
     * @param entry The entry to check against
     * @return Returns true if the lowercase version of the entry is in the
     * search history
     */
    public boolean containsEntry(@NotNull String entry) {
        synchronized (this.history) {
            // Cache should be locked in parallel with history
            return this.cache.contains(entry.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Truncate the history if ihe size exceeds {@link #HISTORY_MAX_SIZE}
     */
    public void truncateHistory() {
        synchronized (this.history) {
            // Cache the max size as it may change, even if it probably won't
            final int maxSize = HISTORY_MAX_SIZE;
            if (this.history.size() < maxSize) {
                return;
            }
            final Iterator<String> descendingIterator = this.history.descendingIterator();
            for (int toRemove = this.history.size() - maxSize; toRemove > 0; toRemove--) {
                this.cache.remove(descendingIterator.next());
                descendingIterator.remove();
            }
        }
    }

}
