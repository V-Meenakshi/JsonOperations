package com.assignment.jsondataset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response body of { POST /api/dataset/{datasetName}/record}. */
@Schema(description = "Confirmation that a record was stored")
public record InsertRecordResponse(
        @Schema(example = "Record added successfully") String message,
        @Schema(example = "employee_dataset") String dataset,
        @Schema(example = "1") long recordId) {

    public static InsertRecordResponse of(String dataset, long recordId) {
        return new InsertRecordResponse("Record added successfully", dataset, recordId);
    }
}
