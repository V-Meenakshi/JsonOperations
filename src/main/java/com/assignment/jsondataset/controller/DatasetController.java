package com.assignment.jsondataset.controller;

import com.assignment.jsondataset.dto.DatasetQuery;
import com.assignment.jsondataset.dto.InsertRecordResponse;
import com.assignment.jsondataset.dto.QueryResponse;
import com.assignment.jsondataset.service.DatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/dataset", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Dataset", description = "Insert JSON records and query them with group-by / sort-by")
public class DatasetController {

    private static final String DATASET_NAME_REGEX = "^[A-Za-z0-9_-]{1,100}$";
    private static final String DATASET_NAME_MESSAGE =
            "Dataset name must be 1-100 characters: letters, digits, '_' or '-'.";
    private static final String PROBLEM_JSON = "application/problem+json";

    private final DatasetService datasetService;

    @Operation(
            summary = "Insert a record into a dataset",
            description = "Stores the JSON object in the dataset (created implicitly on first insert). "
                    + "The object must contain an integer `id`, unique within the dataset. "
                    + "All other fields are free-form.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(type = "object"),
                            examples = @ExampleObject(name = "Employee", value = """
                                    {"id": 1, "name": "John Doe", "age": 30, "department": "Engineering"}"""))))
    @ApiResponse(responseCode = "201", description = "Record added")
    @ApiResponse(responseCode = "400", description = "Malformed JSON, missing/non-integer `id`, or invalid dataset name",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A record with this `id` already exists in the dataset",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{datasetName}/record", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InsertRecordResponse> insertRecord(
            @Parameter(description = "Dataset name (letters, digits, '_' or '-'; max 100)", example = "employee_dataset")
            @PathVariable @Pattern(regexp = DATASET_NAME_REGEX, message = DATASET_NAME_MESSAGE) String datasetName,
            @RequestBody Map<String, Object> recordBody) {

        long recordId = datasetService.insertRecord(datasetName, recordBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(InsertRecordResponse.of(datasetName, recordId));
    }

    @Operation(
            summary = "Query a dataset with optional group-by and sort-by",
            description = """
                    - `groupBy` only -> `groupedRecords`
                    - `sortBy` only -> `sortedRecords`
                    - both -> `groupedRecords`, with records sorted inside each group
                    - neither -> `records` in insertion order

                    Records missing the sort field are placed last in both directions.""")
    @ApiResponse(responseCode = "200", description = "Query result")
    @ApiResponse(responseCode = "400", description = "Unknown `groupBy`/`sortBy` field, invalid `order`, or invalid dataset name",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Dataset does not exist",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{datasetName}/query")
    public QueryResponse queryDataset(
            @Parameter(description = "Dataset name", example = "employee_dataset")
            @PathVariable @Pattern(regexp = DATASET_NAME_REGEX, message = DATASET_NAME_MESSAGE) String datasetName,
            @Parameter(description = "Field to group records by", example = "department")
            @RequestParam(required = false) String groupBy,
            @Parameter(description = "Field to sort records by", example = "age")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction (case-insensitive)",
                    schema = @Schema(allowableValues = {"asc", "desc"}, defaultValue = "asc"))
            @RequestParam(defaultValue = "asc") String order) {

        return datasetService.queryDataset(datasetName, DatasetQuery.of(groupBy, sortBy, order));
    }
}
