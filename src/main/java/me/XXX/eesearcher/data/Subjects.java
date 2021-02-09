package me.XXX.eesearcher.data;

import com.google.inject.Inject;

/**
 * Utility class, represents IB subjects that are not in Group 1 or Group 2
 */
public final class Subjects {


    // Group 1 (Language)

    // Group 2 (Language Acquisition)

    // Group 3 (Individuals & Societies)
    public final Subject HISTORY;
    public final Subject ECONOMICS;
    public final Subject BUSINESS_MANAGEMENT;
    public final Subject ITGS;
    public final Subject GEOGRAPHY;
    public final Subject PHILOSOPHY;
    public final Subject GLOBAL_POLITICS;
    public final Subject PSYCHOLOGY;
    public final Subject ANTHROPOLOGY;
    public final Subject WORLD_RELIGION;

    // Group 4 (Sciences)
    public final Subject PHYSICS;
    public final Subject CHEMISTRY;
    public final Subject BIOLOGY;
    public final Subject COMPUTER_SCIENCE;
    public final Subject SPORT_EXERCISE_HEALTH_SCIENCE;
    public final Subject DESIGN_TECH;
    // Group 5 (Mathematics)
    public final Subject GENERIC_MATH;
    public final Subject MATH_AA_2020;
    public final Subject MATH_AI_2020;
    public final Subject MATH_STUDIES_2020;

    // Group 6 (Arts)
    private final Subject DANCE;
    private final Subject FILM;
    private final Subject MUSIC;
    private final Subject THEATRE;
    private final Subject VISUAL_ARTS;

    private final SubjectDatabase registryInstance;

    @Inject
    public Subjects(SubjectDatabase database) {
        this.registryInstance = database;
        HISTORY = group3("History", true);
        ECONOMICS = group3("Economics", true, "Econ");
        BUSINESS_MANAGEMENT = group3("Business Management", true, "BM");
        ITGS = group3("ITGS", true, "Information Technology in Global Societies");
        GEOGRAPHY = group3("Geography", true, "Geo");
        PHILOSOPHY = group3("Philosophy", true);
        GLOBAL_POLITICS = group3("Global Politics", true, "Politics");
        PSYCHOLOGY = group3("Psychology", true, "Psych");
        ANTHROPOLOGY = group3("Anthropology", true, "Social and Cultural Anthropology");
        WORLD_RELIGION = group3("World Religion", true, "Religion");

        PHYSICS = group4("Physics", true);
        CHEMISTRY = group4("Chemistry", true);
        BIOLOGY = group4("Biology", true);
        COMPUTER_SCIENCE = group4("Computer Science", true, "CS");
        SPORT_EXERCISE_HEALTH_SCIENCE = group4("Sport, Exercise and Health Science", true);
        DESIGN_TECH = group4("Design Technology", true, "DT", "Design Tech");

        GENERIC_MATH = group5("Mathematics", true, "Math");
        MATH_AA_2020 = group5("Mathematics: Analysis and Approaches", true, "Math AA");
        MATH_AI_2020 = group5("Mathematics: Applications and Interpretation", true, "Math AI");
        MATH_STUDIES_2020 = group5("Math Studies", true,

                "Mathematics: Analysis and Approaches",
                "Math AA",
                "Mathematics: Applications and Interpretation",
                "Math AI");

        FILM = group6("Film", true);
        MUSIC = group6("Music", true);
        DANCE = group6("Dance", true);
        THEATRE = group6("Theatre", true);
        VISUAL_ARTS = group6("Visual Arts", true,"VA");

    }

    private Subject group1(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 1, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }

    private Subject group2(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 2, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }

    private Subject group3(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 3, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }

    private Subject group4(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 4, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }

    private Subject group5(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 5, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }

    private Subject group6(final String displayName, final boolean active, final String... aliases) {
        final Subject s = new Subject((byte) 6, displayName, active, aliases);
        registryInstance.registerSubject(s);
        return s;
    }


}
