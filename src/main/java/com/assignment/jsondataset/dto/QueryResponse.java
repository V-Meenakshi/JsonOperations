package com.assignment.jsondataset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * Response body of {@code GET /api/dataset/{datasetName}/query}.
 * Exactly one property is populated, depending on the operation requested:
 * {@code groupedRecords} (group-by), {@code sortedRecords} (sort-by only) or
 * {@code records} (no operation). Unused properties are omitted from the JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Query result; exactly one of the three properties is present")
public record QueryResponse(
        @Schema(description = "Present when groupBy was given: records keyed by the group value")
        Map<String, List<Map<String, Object>>> groupedRecords,
        @Schema(description = "Present when only sortBy was given")
        List<Map<String, Object>> sortedRecords,
        @Schema(description = "Present when neither groupBy nor sortBy was given")
        List<Map<String, Object>> records) {

    public static QueryResponse grouped(Map<String, List<Map<String, Object>>> groupedRecords) {
        return new QueryResponse(groupedRecords, null, null);
    }

    public static QueryResponse sorted(List<Map<String, Object>> sortedRecords) {
        return new QueryResponse(null, sortedRecords, null);
    }

    public static QueryResponse plain(List<Map<String, Object>> records) {
        return new QueryResponse(null, null, records);
    }
}
