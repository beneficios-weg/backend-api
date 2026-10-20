package com.weg.benefits.catalog;

import java.util.List;

/** Stable category colors returned to clients and accepted by catalog writes. */
public final class CategoryPalette {
    public record ColorOption(String name, String hex) { }

    private static final List<ColorOption> COLORS = List.of(
            new ColorOption("green", "#1B5E20"),
            new ColorOption("blue", "#0D47A1"),
            new ColorOption("purple", "#4A148C"),
            new ColorOption("red", "#B71C1C"),
            new ColorOption("orange", "#E65100"),
            new ColorOption("teal", "#006064"),
            new ColorOption("brown", "#4E342E"),
            new ColorOption("blue-grey", "#37474F"));

    private CategoryPalette() { }

    public static List<ColorOption> colors() { return COLORS; }

    public static boolean containsHex(String value) {
        return value != null && COLORS.stream().anyMatch(option -> option.hex().equalsIgnoreCase(value));
    }
}
