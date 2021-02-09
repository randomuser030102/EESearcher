package me.XXX.eesearcher.data;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.XXX.eesearcher.common.ExamSession;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Represents the characteristics of the ExtendedEssay.
 * This class is immutable and is therefore thread-safe.
 */
public final class IndexData {

    private static final Cache<Integer, IndexData> CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(2)
            .weakValues()
            .expireAfterAccess(Duration.ofMinutes(1))
            .build();

    public final int uniqueID;
    private final Subject subject;
    private final String researchQuestion;
    private final String title;
    private final ExamSession examSession;

    private IndexData(@NotNull final String title,
                      @NotNull final Subject subject,
                      @NotNull final String researchQuestion,
                      @NotNull final ExamSession examSession,
                      final int uniqueID) {
        this.subject = subject;
        this.title = title;
        this.researchQuestion = researchQuestion;
        this.examSession = examSession;
        this.uniqueID = uniqueID;
    }

    public static int getUniqueID(@NotNull final String title,
                                  @NotNull final Subject subject,
                                  @NotNull final String researchQuestion,
                                  @NotNull final ExamSession examSession) {

        int result = subject.hashCode();
        result = 31 * result + researchQuestion.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + examSession.hashCode();
        return result;
    }

    /**
     * Obtain an {@link IndexData} instance from a given set of characteristics.
     * @param title The title of the essay
     * @param subject The subject of the essay
     * @param researchQuestion The research question of the essay
     * @param examSession The exam session of the essay
     * @return Returns a cached {@link IndexData} instance or instantiates a new one
     */
    public static @NotNull IndexData from(@NotNull final String title,
                                          @NotNull final Subject subject,
                                          @NotNull final String researchQuestion,
                                          @NotNull final ExamSession examSession) {
        final int hash = getUniqueID(title, subject, researchQuestion, examSession);
        synchronized (CACHE) {
            IndexData indexData = CACHE.getIfPresent(hash);
            if (indexData == null) {
                indexData = new IndexData(title, subject, researchQuestion, examSession, hash);
                CACHE.put(hash, indexData);
            }
            return indexData;
        }
    }

    /**
     * Get the UniqueID for these characteristics. This class implements the
     * UniqueID as the {@link #hashCode()}
     *
     * @return Returns an int representing this object's unique id
     */
    public int getUniqueID() {
        return uniqueID;
    }

    public @NotNull Subject getSubject() {
        return subject;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull String getResearchQuestion() {
        return researchQuestion;
    }

    public @NotNull ExamSession getExamSession() {
        return examSession;
    }

    @Override
    public String toString() {
        return "IndexData{" +
                "subject=" + subject +
                ", researchQuestion='" + researchQuestion + '\'' +
                ", title='" + title + '\'' +
                ", examSession=" + examSession +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexData other = (IndexData) o;
        return this.uniqueID == other.uniqueID;
    }

    @Override
    public int hashCode() {
        return this.uniqueID;
    }
}
