package com.assignment.jsondataset.integration;

import com.assignment.jsondataset.repository.RecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end test (real controller, service and H2 database) replaying the assignment's examples. */
@SpringBootTest
@AutoConfigureMockMvc
class DatasetApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordRepository repository;

    @BeforeEach
    void cleanDatabase() throws Exception {
        repository.deleteAll();
        insert("employee_dataset", "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}")
                .andExpect(status().isCreated());
        insert("employee_dataset", "{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"}")
                .andExpect(status().isCreated());
        insert("employee_dataset", "{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void insertResponseMatchesSpecification() throws Exception {
        insert("other_dataset", "{\"id\":10,\"name\":\"Bob\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Record added successfully"))
                .andExpect(jsonPath("$.dataset").value("other_dataset"))
                .andExpect(jsonPath("$.recordId").value(10));
    }

    @Test
    void groupByDepartmentMatchesSpecification() throws Exception {
        query("employee_dataset", "groupBy=department")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords.Engineering.length()").value(2))
                .andExpect(jsonPath("$.groupedRecords.Engineering[0].name").value("John Doe"))
                .andExpect(jsonPath("$.groupedRecords.Engineering[1].name").value("Jane Smith"))
                .andExpect(jsonPath("$.groupedRecords.Marketing.length()").value(1))
                .andExpect(jsonPath("$.groupedRecords.Marketing[0].name").value("Alice Brown"));
    }

    @Test
    void sortByAgeAscendingMatchesSpecification() throws Exception {
        query("employee_dataset", "sortBy=age&order=asc")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[0].id").value(2))
                .andExpect(jsonPath("$.sortedRecords[1].id").value(3))
                .andExpect(jsonPath("$.sortedRecords[2].id").value(1));
    }

    @Test
    void sortByAgeDescending() throws Exception {
        query("employee_dataset", "sortBy=age&order=desc")
                .andExpect(jsonPath("$.sortedRecords[0].id").value(1))
                .andExpect(jsonPath("$.sortedRecords[2].id").value(2));
    }

    @Test
    void groupAndSortCombine() throws Exception {
        query("employee_dataset", "groupBy=department&sortBy=age&order=asc")
                .andExpect(jsonPath("$.groupedRecords.Engineering[0].name").value("Jane Smith"))
                .andExpect(jsonPath("$.groupedRecords.Engineering[1].name").value("John Doe"));
    }

    @Test
    void duplicateIdReturns409() throws Exception {
        insert("employee_dataset", "{\"id\":1,\"name\":\"Clone\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void sameIdInAnotherDatasetIsAllowed() throws Exception {
        insert("another_dataset", "{\"id\":1,\"name\":\"Clone\"}").andExpect(status().isCreated());
    }

    @Test
    void unknownDatasetReturns404() throws Exception {
        query("does_not_exist", "groupBy=department").andExpect(status().isNotFound());
    }

    @Test
    void unknownFieldReturns400() throws Exception {
        query("employee_dataset", "groupBy=salary").andExpect(status().isBadRequest());
        query("employee_dataset", "sortBy=salary").andExpect(status().isBadRequest());
    }

    @Test
    void invalidOrderReturns400() throws Exception {
        query("employee_dataset", "sortBy=age&order=sideways").andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        insert("employee_dataset", "{broken").andExpect(status().isBadRequest());
    }

    private ResultActions insert(String dataset, String json) throws Exception {
        return mockMvc.perform(post("/api/dataset/{d}/record", dataset)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions query(String dataset, String queryString) throws Exception {
        return mockMvc.perform(get("/api/dataset/{d}/query?" + queryString, dataset));
    }
}
