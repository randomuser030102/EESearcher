package me.XXX.eesearcher.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.zaxxer.hikari.pool.HikariPool;
import me.XXX.eesearcher.data.*;
import me.andrewandy.eesearcher.data.*;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;

/**
 * Represents a controller for queries for {@link IndexData}. All methods in this class are
 * thread-safe and can be called from multiple threads.
 * <p>
 * This controller will periodically
 * synchronise cached {@link IndexData} back to the database; however, this process is not
 * scheduled. In fact, synchronization back to the database for cached objects <strong>only</strong>
 * occurs when there are no strong references to them.
 * </p>
 */
public final class IndexDataController {

    public static final int MAX_QUERY_CACHE_SIZE = 20;

    private final Cache<QueryParameters, Set<SearchResult>> queryCache = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .maximumSize(MAX_QUERY_CACHE_SIZE)
            .build();
    private final Map<QueryParameters, CompletableFuture<Set<SearchResult>>> pendingQueries = new ConcurrentHashMap<>();

    @Inject
    private Parser parser;
    @Inject
    private DataUtil dataUtil;
    @Inject
    @Named("internal-pool")
    private HikariPool connectionPool;
    @Inject
    private ScheduledExecutorService executorService;

    private final Cache<IndexData, Essay> indexDataCache = CacheBuilder.newBuilder()
            // Expecting 2 concurrent threads, no more.
            .concurrencyLevel(2)
            // Remove entries if they are weakly referenced
            .weakKeys()
            .weakValues()
            .<IndexData, Essay>removalListener(listener -> {
                final Essay value = listener.getValue();
                if (value == null) {
                    return;
                }
                // Delegate the save task to the IO thread pool so as to keep the thread managing
                // this map free to clean up other entries.
                executorService.execute(() -> {
                    // Close the essay's underlying PDF using try-with resources
                    try (Essay essay = value; Connection connection = connectionPool.getConnection();
                         PreparedStatement statement = dataUtil.newEntry(connection, essay, true)) {
                        statement.execute();
                    } catch (SQLException | IOException ex) {
                        // Should never happen!
                        ex.printStackTrace();
                    }
                });
            }).build();


    /**
     * Perform a query asynchronously from the current thread.
     * @param queryParameters The query parameters
     * @return Returns a {@link CompletableFuture} representing the state of execution
     * @see #performQueryAsync(QueryParameters)
     */
    private CompletableFuture<Set<SearchResult>> performQueryAsync(@NotNull QueryParameters queryParameters) {
        final CompletableFuture<Set<SearchResult>> pending = pendingQueries.get(queryParameters);
        if (pending != null) {
            return pending;
        }
        return CompletableFuture.completedFuture(performQuerySync(queryParameters));
    }

    /**
     * Perform a query to the database on the current thread.
     * @param queryParameters The query parameters
     * @return Returns a never-null {@link Set} of {@link SearchResult}s
     * @see #performQueryAsync(QueryParameters)
     */
    private Set<SearchResult> performQuerySync(QueryParameters queryParameters) {
        final CompletableFuture<Set<SearchResult>> completableFuture = new CompletableFuture<>();
        pendingQueries.put(queryParameters, completableFuture);
        final Set<SearchResult> results = new HashSet<>();
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement query = dataUtil.newSearch(connection, queryParameters, -1);
             ResultSet resultSet = query.executeQuery()) {
            while (resultSet.next()) {
                Essay essay;
                try {
                    essay = dataUtil.extractEssay(resultSet, this::getCachedEssay);
                    indexDataCache.put(essay.getIndexData(), essay);
                    results.add(new SearchResult(essay, Collections.emptyList()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            completableFuture.completeExceptionally(ex);
        }
        queryCache.put(queryParameters, results);
        pendingQueries.remove(queryParameters);
        if (!completableFuture.isCompletedExceptionally()) {
            completableFuture.complete(results);
        }
        return results;
    }

    /**
     * Get a copy of {@link Essay}s instances which this controller has cached. Changes to
     * the returned Set will not be reflected in this cache. The same is true vice-versa, changes
     * to the cache after the set has been returned will not affect the returned set
     * @return Returns a {@link Set<Essay>} which is a copy of the cached instances
     */
    public @NotNull Set<@NotNull Essay> getCachedEssays() {
        return new HashSet<>(this.indexDataCache.asMap().values());
    }

    /**
     * Get an essay based on its characteristics represented by an {@link IndexData} instance
     * @param indexData The characteristics of the essay represented by an IndexData instance
     * @return Returns a never-null {@link Optional} which is populated by the {@link Essay}
     * instance if cached
     */
    public @NotNull Optional<@NotNull Essay> getCachedEssay(@NotNull IndexData indexData) {
        return Optional.ofNullable(indexDataCache.getIfPresent(indexData));
    }

    /**
     * Perform a query based on some parameters. This method will attempt to look for a cached
     * result before performing a query asynchronously.
     *
     * @param queryParameters An instance of the {@link QueryParameters} to use when searching
     * @return Returns a {@link CompletableFuture} which contains a {@link Set<SearchResult>}
     * corresponding to the query parameters.
     */
    public synchronized @NotNull CompletableFuture<@NotNull Set<@NotNull SearchResult>> performQuery(@NotNull QueryParameters queryParameters) {
        final Optional<Set<SearchResult>> optionalSearchResult = getCachedResult(queryParameters);
        return optionalSearchResult.map(CompletableFuture::completedFuture).orElseGet(() -> performQueryAsync(queryParameters));
    }

    /**
     * Get the results of a given query from the cache
     * @param searchQueryParameters An instance of the {@link QueryParameters} to use when searching
     * @return Returns an {@link Optional} which is populated by a {@link Set<SearchResult>} if the
     * query was previously performed and cached in the database.
     */
    public @NotNull Optional<@NotNull Set<@NotNull SearchResult>> getCachedResult(@NotNull QueryParameters searchQueryParameters) {
        return Optional.ofNullable(queryCache.getIfPresent(searchQueryParameters));
    }

    /**
     * Request for files to be indexed and subsequently cached. Any existing files which were
     * already cached will be overwritten.
     *
     * @param files        A {@link Collection} of files to be indexed
     * @param onCompletion A listener for every time a file has been processed and whether it was indexed successfully
     * @return Returns a never-null {@link CompletableFuture} which represents current state of execution. The result of
     * the future will always be null.
     */
    public @NotNull CompletableFuture<Void> performIndexing(@NotNull Collection<File> files, BiConsumer<File, Boolean> onCompletion) {
        final Collection<CompletableFuture<Void>> futures = Collections.synchronizedList(new LinkedList<>());
        for (File file : files) {
            final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            futures.add(completableFuture);
            executorService.execute(() -> {
                Exception exception = null;
                try {
                    // Load the PDF from the disk
                    final PDFParser pdfParser = new PDFParser(new RandomAccessBufferedFileInputStream(file));
                    // Parse the PDF
                    pdfParser.parse();
                    // Attempt to parse an essay from the PDF document.
                    Essay essay = parser.parseDocument(pdfParser);
                    // Cache the essay against its index data
                    indexDataCache.put(essay.getIndexData(), essay);
                    // Merge data values into database, over-writing existing values
                    dataUtil.newEntry(connectionPool.getConnection(), essay, true).executeUpdate();
                } catch (IOException | SQLException | IllegalArgumentException ex) {
                    // Re-Throw the exception as a runtime exception.
                    exception = new RuntimeException(String.format("Error parsing %s", file), ex);
                } finally {
                    // Run the listener
                    onCompletion.accept(file, exception == null);
                    if (exception != null) {
                        completableFuture.completeExceptionally(exception);
                    } else {
                        completableFuture.complete(null);
                    }
                }
            });
        }
        // Returns a future which represents the execution state of indexing all provided files.
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }


}
