package me.XXX.eesearcher.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.XXX.eesearcher.data.Essay;
import me.XXX.eesearcher.data.IndexData;
import me.XXX.eesearcher.data.Subject;
import me.XXX.eesearcher.data.SubjectDatabase;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parser of {@link Essay}s. All methods in this class are thread-safe and
 * can be called from multiple threads.
 */
@Singleton
public final class Parser {

    /**
     * Regular expressions to match specific characteristics of an Extended Essay
     */
    private static final Pattern LINE_SEPARATOR = Pattern.compile(System.lineSeparator() + "|^\\s*$");
    private static final Pattern SUBJECT_PARSER = Pattern.compile("(Subject:)\\s?(\\w*|\\s)($|\\.|\\s+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESEARCH_QUESTION_PATTERN = Pattern.compile("(Research Question:)");
    private static final Pattern RESEARCH_QUESTION_PARSER = Pattern.compile(".*\\?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PARSER = Pattern.compile("(((Topic|Title):)|^)(\\s?(\\w+|.|\\s)+($|\\.|\\s+))", Pattern.CASE_INSENSITIVE);
    private static final Pattern HACKY_TITLE_PARSER = Pattern.compile("\\b\\w+(?<!(\\.))");
    private static final Pattern EXAM_SESSION_PARSER = Pattern.compile("((may|november)(\\s?([0-9]{4})))", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^(\\w+)(\\s?([ab])|$|\\.)");

    @Inject
    private SubjectDatabase database;

    /**
     * Attempt to parse an essay from a given {@link PDFParser}. This method heavily utilizes
     * regular expressions, it is strongly advised for the max stack size to be increased depending on
     * how much text is on each cover page. For an average of 50-100 words on an A4 sized cover page a
     * stack size of around 2MB should be sufficient.
     *
     * @param parser The PDF parser instance
     * @return Returns an {@link Essay} representing the given PDF
     * @throws IOException              Thrown if an error occurs when parsing the PDF into plaintext
     * @throws IllegalArgumentException Thrown if no Subject, Title or Research Question could be found.
     */
    public Essay parseDocument(final PDFParser parser) throws IOException, IllegalArgumentException {
        final List<String> pages = parseTextByPage(parser);
        final String coverPage = pages.get(0);
        final Matcher subjectMatcher = SUBJECT_PARSER.matcher(coverPage);
        if (!subjectMatcher.find()) {
            throw new IllegalArgumentException("Invalid Essay: No subject found!");
        }

        // Look for a title
        final Matcher titleMatcher = TITLE_PARSER.matcher(coverPage);
        String title = null;
        if (!titleMatcher.find()) {
            final Matcher hacky = HACKY_TITLE_PARSER.matcher(coverPage);
            while (hacky.find()) {
                final String found = hacky.group().trim();
                if (found.charAt(found.length() - 1) == '?') {
                    continue;
                }
                title = found;
            }
            if (title == null) {
                throw new IllegalArgumentException("Invalid Essay: No title/topic found!");
            }
        } else {
            title = titleMatcher.group(5).trim();
        }

        // Look for a research question
        final Matcher rqMatcher = RESEARCH_QUESTION_PARSER.matcher(coverPage);
        if (!rqMatcher.find()) {
            throw new IllegalArgumentException("Invalid Essay: No title/topic found!");
        }
        final String rawResearchQuestion = rqMatcher.group();
        // Strip "Research Question:" from the parsed string if present.
        final String researchQuestion = RESEARCH_QUESTION_PATTERN.matcher(rawResearchQuestion).replaceAll("").trim();

        final String rawSubject = subjectMatcher.group(2).trim();
        // Check if subject is a language
        final Matcher languageMatcher = LANGUAGE_PATTERN.matcher(rawSubject);
        final Subject subject;
        if (languageMatcher.find()) {
            final String language = languageMatcher.group(1).trim();
            // Lookup subject from in-memory SubjectDatabase
            final Optional<Subject> optional = database.getSubjectByName(language);
            subject = optional.orElseGet(() -> {
                // Generate a new Subject
                final Subject newSubject = new Subject((byte) 1, Utils.titleCase(language.toLowerCase(Locale.ROOT)), true);
                database.registerSubject(newSubject);
                return newSubject;
            });
        } else {
            // If not a language, subject should be pre-initialized into the SubjectDatabase
            subject = database.getSubjectByName(rawSubject).orElseThrow(() -> new IllegalArgumentException(String.format("Invalid Subject: %s", rawSubject)));
        }
        final ExamSession session;
        final Matcher sessionMatcher = EXAM_SESSION_PARSER.matcher(coverPage);
        // Attempt to identify the exam session
        if (sessionMatcher.find()) {
            final String rawMonth = sessionMatcher.group(2).trim();
            final String rawYear = sessionMatcher.group(3).trim();
            ExamSession temp;
            try {
                Month month = Month.valueOf(rawMonth.toUpperCase(Locale.ENGLISH));
                int year = Integer.parseInt(rawYear);
                temp = ExamSession.of(month, year);
            } catch (IllegalArgumentException ex) {
                // If error in parsing, fall back to the empty session
                temp = ExamSession.EMPTY_SESSION;
            }
            session = temp;
        } else {
            // If none found, fall back to empty session
            session = ExamSession.EMPTY_SESSION;
        }
        // Initialize index data and returns a new essay instance
        final IndexData data = IndexData.from(title, subject, researchQuestion, session);
        return new Essay(data, parser.getPDDocument());
    }

    public PDFParser parseDocument(final InputStream inputStream) throws IOException {
        final PDFParser parser = new PDFParser(new RandomAccessBuffer(inputStream));
        parser.parse();
        return parser;
    }

    public String parseText(final InputStream inputStream) throws IOException {
        return parseText(parseDocument(inputStream));
    }

    public String parseText(final PDFParser parser) throws IOException {
        final PDFTextStripper pdfStripper = new PDFTextStripper();
        try (final PDDocument document = parser.getPDDocument()) {
            return pdfStripper.getText(document);
        }
    }

    public List<String> parseTextByPage(final PDFParser parser) throws IOException {
        final PDDocument document = parser.getPDDocument();
        final PDFTextStripper stripper = new PDFTextStripper();
        final List<String> pages = new ArrayList<>(document.getNumberOfPages());
        for (int i = 1; i < document.getNumberOfPages(); ) {
            stripper.setStartPage(i++);
            stripper.setEndPage(i);
            final String raw = stripper.getText(document);
            pages.add(raw);
        }
        return pages;
    }

}
