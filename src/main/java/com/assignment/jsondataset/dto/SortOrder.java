package com.assignment.jsondataset.dto;

import com.assignment.jsondataset.exception.InvalidQueryException;

import java.util.Locale;


public enum SortOrder {
    ASC,
    DESC;

    /**
     * Parses the {@code order} query parameter (case-insensitive).
     * A missing or blank value defaults to {@link #ASC}.
     * @throws InvalidQueryException if the value is neither "asc" nor "desc"
     */
    public static SortOrder parse(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> ASC;
            case "desc" -> DESC;
            default -> throw new InvalidQueryException(
                    "Invalid sort order '" + value + "'. Allowed values are 'asc' and 'desc'.");
        };
    }
}
