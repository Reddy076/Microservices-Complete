package com.revature.vault.model.analytics;

import java.time.LocalDateTime;

/**
 * Value object representing the time window for a timeline query.
 *
 * <p>Encapsulates the period label string, start time, and end time so that
 * service methods and tests can reason about time ranges as first-class objects
 * rather than scattered {@code Integer days} parameters and inline string
 * concatenation.</p>
 */
public final class TimelinePeriod {

    private final String label;
    private final LocalDateTime start;
    private final LocalDateTime end;

    private TimelinePeriod(String label, LocalDateTime start, LocalDateTime end) {
        this.label = label;
        this.start = start;
        this.end = end;
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a {@code TimelinePeriod} covering the last {@code days} days up to now.
     *
     * @param days number of past days to include (must be > 0)
     * @return a bounded period
     */
    public static TimelinePeriod lastDays(int days) {
        LocalDateTime now = LocalDateTime.now();
        return new TimelinePeriod("LAST_" + days + "_DAYS", now.minusDays(days), now);
    }

    /**
     * Creates a {@code TimelinePeriod} representing all time from the given
     * earliest event timestamp to now.
     *
     * @param earliest the timestamp of the oldest relevant event
     * @return an unbounded (all-time) period
     */
    public static TimelinePeriod allTime(LocalDateTime earliest) {
        return new TimelinePeriod("ALL_TIME",
                earliest != null ? earliest : LocalDateTime.now(),
                LocalDateTime.now());
    }

    /**
     * Resolves the correct {@code TimelinePeriod} from an optional {@code days}
     * parameter — the same logic used across all timeline service methods.
     *
     * @param days     optional days filter (null or ≤ 0 → all time)
     * @param earliest earliest known event timestamp for all-time mode
     * @return the resolved period
     */
    public static TimelinePeriod resolve(Integer days, LocalDateTime earliest) {
        if (days != null && days > 0) {
            return lastDays(days);
        }
        return allTime(earliest);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the display label, e.g. {@code "LAST_30_DAYS"} or {@code "ALL_TIME"}. */
    public String getLabel() {
        return label;
    }

    /** Returns the start of this period (inclusive). */
    public LocalDateTime getStart() {
        return start;
    }

    /** Returns the end of this period (inclusive, always {@link LocalDateTime#now()} at construction). */
    public LocalDateTime getEnd() {
        return end;
    }

    /**
     * Returns the number of calendar days spanned by this period.
     * Used to compute accurate averages in the stats endpoint.
     */
    public long getDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
    }

    @Override
    public String toString() {
        return "TimelinePeriod{label='" + label + "', start=" + start + ", end=" + end + "}";
    }
}
