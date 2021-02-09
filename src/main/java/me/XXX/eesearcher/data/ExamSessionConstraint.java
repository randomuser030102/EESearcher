package me.XXX.eesearcher.data;

import me.XXX.eesearcher.common.ExamSession;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ExamSessionConstraint {

    public final ExamSession examSession;
    public final Type type;

    public ExamSessionConstraint(@NotNull final ExamSession examSession, @NotNull final Type type) {
        this.examSession = Objects.requireNonNull(examSession);
        this.type = Objects.requireNonNull(type);
    }

    public ExamSessionConstraint(@NotNull ExamSessionConstraint other) {
        this.examSession = other.examSession;
        this.type = other.type;
    }

    public ExamSession getExamSession() {
        return examSession;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        BEFORE, AFTER, ONLY
    }
}
