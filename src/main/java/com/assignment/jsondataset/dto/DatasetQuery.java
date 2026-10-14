package com.assignment.jsondataset.dto;


public record DatasetQuery(String groupBy, String sortBy, SortOrder order) {

    /** Builds a query from raw request parameters; blank fields are treated as "not provided". */
    public static DatasetQuery of(String groupBy, String sortBy, String order) {
        return new DatasetQuery(blankToNull(groupBy), blankToNull(sortBy), SortOrder.parse(order));
    }

    public boolean hasGroupBy() {
        return groupBy != null;
    }

    public boolean hasSortBy() {
        return sortBy != null;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
