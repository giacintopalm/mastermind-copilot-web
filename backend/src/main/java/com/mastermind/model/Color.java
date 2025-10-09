package com.mastermind.model;

/**
 * Enumeration representing the available colors in the Mastermind game.
 * Maps directly to the TypeScript Color type from the frontend.
 */
public enum Color {
    RED("red"),
    BLUE("blue"), 
    GREEN("green"),
    YELLOW("yellow"),
    PURPLE("purple"),
    CYAN("cyan");

    private final String value;

    Color(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Convert string value to Color enum.
     * @param value String representation of color
     * @return Color enum value
     * @throws IllegalArgumentException if color is not valid
     */
    public static Color fromString(String value) {
        for (Color color : Color.values()) {
            if (color.value.equalsIgnoreCase(value)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Invalid color: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}