package me.XXX.eesearcher.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.zaxxer.hikari.pool.HikariPool;
import me.XXX.eesearcher.common.ExamSession;
import me.XXX.eesearcher.common.Parser;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.h2.Driver;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * Utility class which is used to setup the backend database.
 */
public class DataUtil {

    // %1
    public static final String TABLE_NAME = "EEData";
    // %2
    public static final String COLUMN_UUID = "hashcode";
    // %3
    public static final String COLUMN_TITLE = "title";
    // %4
    public static final String COLUMN_SUBJECT = "subject";
    // %5
    public static final String COLUMN_EXAM_YEAR = "exam_year";
    // %6
    public static final String COLUMN_RESEARCH_QUESTION = "research_question";
    // %7
    public static final String COLUMN_PDF = "pdf";

    @Inject
    @Named("internal-pool")
    private HikariPool pool;
    @Inject
    private Parser parser;
    @Inject
    private SubjectDatabase subjectDatabase;

    private static String generateSqlConstraints(@NotNull QueryParameters parameters, int maxQueries) {

        final String pattern = parameters.regex;
        final char[] flags = parameters.flags;
        final Set<Subject> subjects = parameters.subjects;
        final ExamSessionConstraint sessionConstraint = parameters.examSessionConstraint;
        final String[] rawSubjects;

        if (subjects == null) {
            rawSubjects = new String[0];
        } else {
            rawSubjects = subjects.stream()
                    .map(Subject::getDisplayName)
                    .map(String::toLowerCase)
                    .distinct()
                    .toArray(String[]::new);
        }
        if (maxQueries == 0 || maxQueries < -1) {
            throw new IllegalArgumentException(String.format("Invalid MaxQueries: %d!", maxQueries));
        }
        final StringJoiner flagJoiner = new StringJoiner(", ", "'", "'");
        for (char c : flags) {
            flagJoiner.add(String.valueOf(c));
        }
        final String appendedFlags = flagJoiner.toString();

        final String limit;

        if (maxQueries != -1) {
            limit = String.format("LIMIT(%d) ", maxQueries);
        } else {
            limit = "";

        }

        final StringBuilder base = new StringBuilder(" WHERE ");
        final StringJoiner constraint = new StringJoiner(" AND ");

        if (!pattern.isEmpty()) {
            final StringJoiner orConstraint = new StringJoiner(" OR ");
            orConstraint.add("REGEXP_LIKE(%3$s, " + pattern + ", " + appendedFlags + ")");
            orConstraint.add("REGEXP_LIKE(%6$s, " + pattern + ", " + appendedFlags + ")");
            constraint.add("( " + orConstraint.toString() + " )");
        }
        if (rawSubjects.length != 0) {
            final StringJoiner joiner = new StringJoiner(" OR ");
            for (String s : rawSubjects) {
                joiner.add(" %4$s LIKE " + s);
            }
            constraint.add("(" + joiner.toString() + ")");
        }
        if (sessionConstraint != null) {
            final String s = "WHERE %1$s %2$s %3$d ";
            final String comparator;
            switch (sessionConstraint.type) {
                case ONLY:
                    comparator = " = ";
                    break;
                case AFTER:
                    comparator = " > ";
                    break;
                case BEFORE:
                    comparator = "< ";
                    break;
                default:
                    throw new IllegalStateException("Unknown session constraint: " + sessionConstraint.type);
            }
            final String localConstraint = String.format(s, COLUMN_EXAM_YEAR, comparator, sessionConstraint.examSession.epochMilli);
            constraint.add(localConstraint);
        }

        final String rawSql = base.append(constraint.toString()).append(limit).toString();
        return String.format(rawSql, TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION, COLUMN_PDF);
    }

    public void initDatabase() throws SQLException {
        Driver.load();
        try (Connection connection = pool.getConnection(); PreparedStatement init = initStatement(connection)) {
            init.execute();
        }
    }

    private @NotNull PreparedStatement initStatement(@NotNull final Connection connection) throws SQLException {
        final String initTable = "CREATE TABLE IF NOT EXISTS %1$s (" +
                "%2$s INT NOT NULL, " +
                "%3$s VARCHAR NOT NULL, " +
                "%4$s VARCHAR NOT NULL, " +
                "%5$s BIGINT NOT NULL, " +
                "%6$s VARCHAR NOT NULL, " +
                "%7$s BINARY NOT NULL, " +
                "UNIQUE(%3$s, %4$s, %5$s, %6$s)," +
                "PRIMARY KEY(%2$s)); ";
        final String initUUIDIndex = "CREATE INDEX IF NOT EXISTS %2$s_index ON %1$s " +
                "(%2$s, %3$s, %4$s, %5$s, %6$s); ";
        final String initExamYearIndex = "CREATE INDEX IF NOT EXISTS %3$s_index ON %1$s " +
                "(%3$s, %2$s, %4$s, %5$s, %6$s); ";
        final String initTitleIndex = "CREATE INDEX IF NOT EXISTS %4$s_index ON %1$s " +
                "(%4$s, %2$s, %3$s, %5$s, %6$s); ";
        final String initResearchQuestionIndex = "CREATE INDEX IF NOT EXISTS %5$s_index ON %1$s " +
                "(%5$s, %2$s, %3$s, %4$s, %6$s); ";
        final String initSubjectIndex = "CREATE INDEX IF NOT EXISTS %6$s_index ON %1$s " +
                "(%6$s, %2$s, %3$s, %4$s, %5$s); ";

        final String sql = String.format(initTable + initUUIDIndex + initExamYearIndex + initTitleIndex + initResearchQuestionIndex + initSubjectIndex,
                TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION, COLUMN_PDF);
        return connection.prepareStatement(sql);
    }

    public @NotNull Essay extractEssay(@NotNull ResultSet resultSet, @NotNull Function<IndexData, Optional<Essay>> cacheResolver) throws SQLException, IOException {
        final String rawSubject = resultSet.getString(COLUMN_SUBJECT);
        final Optional<Subject> optionalSubject = subjectDatabase.getSubjectByName(rawSubject);
        final byte[] rawPDF = resultSet.getBytes(COLUMN_PDF);
        // Just parse parse / register the subject using the parser as the indices are clearly invalid.
        if (optionalSubject.isEmpty()) {
            final PDFParser pdfParser = new PDFParser(new RandomAccessBuffer(rawPDF));
            pdfParser.parse();
            return parser.parseDocument(pdfParser);
        }
        final Subject subject = optionalSubject.get();
        final String title = resultSet.getString(COLUMN_TITLE);
        final String researchQuestion = resultSet.getString(COLUMN_RESEARCH_QUESTION);
        final long examSession = resultSet.getLong(COLUMN_EXAM_YEAR);
        final ExamSession session = ExamSession.of(examSession);
        final IndexData indexData = IndexData.from(title, subject, researchQuestion, session);
        final Optional<Essay> optionalEssay = cacheResolver.apply(indexData);
        return optionalEssay.orElseGet(() -> new Essay(indexData, rawPDF));
    }

    public @NotNull PreparedStatement newSearch(@NotNull Connection connection, @NotNull QueryParameters parameters, int maxQueries) throws SQLException {
        final String constraint = generateSqlConstraints(parameters, maxQueries);
        final String raw = "SELECT %3$s, %4$s, %5$s, %6$s, %7$s from %1$s" + constraint + ";";
        final String sql = String.format(raw, TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION, COLUMN_PDF);
        return connection.prepareStatement(sql);
    }

    public @NotNull PreparedStatement newEntry(@NotNull final Connection connection,
                                               @NotNull final Essay essay,
                                               final boolean includePDFData) throws SQLException {
        final IndexData data = essay.getIndexData();
        final String sql;
        if (!includePDFData) {
            String s = "MERGE INTO %1$s (%2$s, %3$s, %4$s, %5$s, %6$s) VALUES(?, ?, ?, ?, ?);";
            sql = String.format(s, TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION);
        } else {
            String s = "MERGE INTO %1$s (%2$s, %3$s, %4$s, %5$s, %6$s, %7$s) VALUES(?, ?, ?, ?, ?, ?);";
            sql = String.format(s, TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION, COLUMN_PDF);
        }
        final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, data.getUniqueID());
        preparedStatement.setString(2, data.getTitle());
        preparedStatement.setString(3, data.getSubject().getDisplayName());
        preparedStatement.setLong(4, data.getExamSession().epochMilli);
        preparedStatement.setString(5, data.getResearchQuestion());
        if (includePDFData) {
            final PDDocument document = essay.getDocument();
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                document.save(bos);
            } catch (IOException ex) {
                // Should never happen!
                throw new RuntimeException(ex);
            }
            preparedStatement.setBytes(6, bos.toByteArray());
        }
        return preparedStatement;
    }

    public @NotNull PreparedStatement newDeletion(@NotNull final Connection connection, @NotNull IndexData data) throws SQLException {
        final String rawSql = "DELETE FROM %1$s WHERE %2$s=?;";
        final String sql = String.format(rawSql, TABLE_NAME, COLUMN_UUID);
        final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, data.getUniqueID());
        return preparedStatement;
    }

    public @NotNull PreparedStatement newDeletion(@NotNull final Connection connection,
                                                  @NotNull final QueryParameters parameters) throws SQLException {
        final String rawSQL = "DELETE FROM %1$s" + generateSqlConstraints(parameters, -1) + ";";
        final String sql = String.format(rawSQL, TABLE_NAME, COLUMN_UUID, COLUMN_TITLE, COLUMN_SUBJECT, COLUMN_EXAM_YEAR, COLUMN_RESEARCH_QUESTION);
        return connection.prepareStatement(sql);
    }

}
