package me.XXX.eesearcher.common;

import org.jetbrains.annotations.NotNull;

import java.time.Month;
import java.util.Calendar;
import java.util.GregorianCalendar;

public final class ExamSession implements Comparable<ExamSession> {

    public static final ExamSession EMPTY_SESSION = new ExamSession();
    public static final long EMPTY_EPOCH_MILLI = EMPTY_SESSION.epochMilli;

    public final int year;
    public final Month month;
    public final String displayName;
    public final long epochMilli;

    private final int hashCode;

    private ExamSession() {
        this.year = 1970;
        this.epochMilli = 0;
        this.month = Month.JANUARY;
        this.hashCode = Integer.MIN_VALUE;
        this.displayName = "Unknown";
    }

    private ExamSession(final int year, final Month month) {
        this.year = year;
        if (month != Month.MAY && month != Month.NOVEMBER) {
            throw new IllegalArgumentException(String.format("Invalid exam session month %s", month));
        }
        // 1974 was the year the EE was first introduced.
        if (year < 1974 || year > Calendar.getInstance().get(Calendar.YEAR)) {
            throw new IllegalArgumentException("Possible year for an exam session: " + year);
        }
        this.month = month;

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.set(year, month.getValue() - 1);
        this.epochMilli = gregorianCalendar.getTimeInMillis();

        // Example: M21 or N20
        this.displayName = (month == Month.MAY ? "M" : "N") + year % 1000;
        this.hashCode = 13 * this.displayName.hashCode();
    }

    public static ExamSession of(final long epochMilli) {
        if (epochMilli == 0) {
            return EMPTY_SESSION;
        }
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(epochMilli);
        final int year = gregorianCalendar.get(Calendar.YEAR);
        final int intMonth = gregorianCalendar.get(Calendar.MONTH);
        final Month month = Month.of(intMonth + 1);
        return new ExamSession(year, month);
    }

    public static @NotNull ExamSession of(final Month month, int year) {
        return new ExamSession(year, month);
    }

    public static @NotNull ExamSession of(final String displayName) throws IllegalArgumentException {
        if (displayName.equals(EMPTY_SESSION.displayName)) {
            return EMPTY_SESSION;
        }
        final char rawMonth = displayName.charAt(0);
        final Month month;
        switch (rawMonth) {
            case 'M':
            case 'm':
                month = Month.MAY;
                break;
            case 'N':
            case 'n':
                month = Month.NOVEMBER;
                break;
            default:
                throw new IllegalArgumentException(String.format("Invalid exam session month %s", rawMonth));
        }
        final int parsed = Integer.parseInt(displayName.substring(1));
        // Determine century.
        final int year = (parsed > 50 ? 1900 : 2000) + parsed;
        return of(month, year);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExamSession that = (ExamSession) o;
        return this.epochMilli == that.epochMilli;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public int compareTo(@NotNull final ExamSession o) {
        return Long.compare(this.epochMilli, o.epochMilli);
    }
}
