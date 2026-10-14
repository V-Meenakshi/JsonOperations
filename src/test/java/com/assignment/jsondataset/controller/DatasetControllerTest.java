package com.assignment.jsondataset.controller;

import com.assignment.jsondataset.dto.DatasetQuery;
import com.assignment.jsondataset.dto.QueryResponse;
import com.assignment.jsondataset.exception.DatasetNotFoundException;
import com.assignment.jsondataset.exception.DuplicateRecordException;
import com.assignment.jsondataset.exception.InvalidQueryException;
import com.assignment.jsondataset.service.DatasetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    private static final String RECORD_URL = "/api/dataset/employees/record";
    private static final String QUERY_URL = "/api/dataset/employees/query";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DatasetService datasetService;

    // ------------------------------------------------------------ POST record

    @Test
    void insertReturns201WithSpecResponseBody() throws Exception {
        when(datasetService.insertRecord(eq("employees"), any())).thenReturn(1L);

        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Record added successfully"))
                .andExpect(jsonPath("$.dataset").value("employees"))
                .andExpect(jsonPath("$.recordId").value(1));

        verify(datasetService).insertRecord("employees",
                Map.of("id", 1, "name", "John Doe", "age", 30, "department", "Engineering"));
    }

    @Test
    void insertWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verify(datasetService, never()).insertRecord(any(), any());
    }

    @Test
    void insertWithJsonArrayBodyReturns400() throws Exception {
        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON).content("[1,2]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insertWithEmptyBodyReturns400() throws Exception {
        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insertWithWrongContentTypeReturns415() throws Exception {
        mockMvc.perform(post(RECORD_URL).contentType(MediaType.TEXT_PLAIN).content("id=1"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void insertWithInvalidDatasetNameReturns400() throws Exception {
        mockMvc.perform(post("/api/dataset/bad$name/record").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isBadRequest());
        verify(datasetService, never()).insertRecord(any(), any());
    }

    @Test
    void insertMissingIdReturns400WithProblemBody() throws Exception {
        when(datasetService.insertRecord(any(), any()))
                .thenThrow(new InvalidQueryException("Record is missing the mandatory 'id' field."));

        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Record is missing the mandatory 'id' field."));
    }

    @Test
    void insertDuplicateReturns409() throws Exception {
        when(datasetService.insertRecord(any(), any())).thenThrow(new DuplicateRecordException("already exists"));

        mockMvc.perform(post(RECORD_URL).contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("already exists"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete(RECORD_URL)).andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------ GET query

    @Test
    void queryGroupByReturnsGroupedRecords() throws Exception {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        grouped.put("Engineering", List.of(Map.of("id", 1, "name", "John Doe")));
        when(datasetService.queryDataset(eq("employees"), any())).thenReturn(QueryResponse.grouped(grouped));

        mockMvc.perform(get(QUERY_URL).param("groupBy", "department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords.Engineering[0].name").value("John Doe"))
                .andExpect(jsonPath("$.sortedRecords").doesNotExist())
                .andExpect(jsonPath("$.records").doesNotExist());

        verify(datasetService).queryDataset("employees", new DatasetQuery("department", null,
                com.assignment.jsondataset.dto.SortOrder.ASC));
    }

    @Test
    void querySortByReturnsSortedRecords() throws Exception {
        when(datasetService.queryDataset(eq("employees"), any()))
                .thenReturn(QueryResponse.sorted(List.of(Map.of("id", 2, "age", 25), Map.of("id", 1, "age", 30))));

        mockMvc.perform(get(QUERY_URL).param("sortBy", "age").param("order", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[0].age").value(25))
                .andExpect(jsonPath("$.groupedRecords").doesNotExist());

        verify(datasetService).queryDataset("employees", new DatasetQuery(null, "age",
                com.assignment.jsondataset.dto.SortOrder.DESC));
    }

    @Test
    void queryWithInvalidOrderReturns400() throws Exception {
        mockMvc.perform(get(QUERY_URL).param("sortBy", "age").param("order", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("sideways")));
        verify(datasetService, never()).queryDataset(any(), any());
    }

    @Test
    void queryUnknownDatasetReturns404() throws Exception {
        when(datasetService.queryDataset(eq("employees"), any()))
                .thenThrow(new DatasetNotFoundException("Dataset 'employees' does not exist."));

        mockMvc.perform(get(QUERY_URL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingDetails() throws Exception {
        when(datasetService.queryDataset(any(), any())).thenThrow(new IllegalStateException("secret internals"));

        mockMvc.perform(get(QUERY_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
