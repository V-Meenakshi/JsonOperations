package com.assignment.jsondataset.service;

import com.assignment.jsondataset.dto.DatasetQuery;
import com.assignment.jsondataset.dto.QueryResponse;
import com.assignment.jsondataset.entity.DatasetRecord;
import com.assignment.jsondataset.exception.DatasetNotFoundException;
import com.assignment.jsondataset.exception.DuplicateRecordException;
import com.assignment.jsondataset.exception.InvalidQueryException;
import com.assignment.jsondataset.repository.RecordRepository;
import com.assignment.jsondataset.strategy.AscendingSortStrategy;
import com.assignment.jsondataset.strategy.DescendingSortStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceImplTest {

    private static final String DATASET = "employees";

    @Mock
    private RecordRepository recordRepository;

    private DatasetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DatasetServiceImpl(
                recordRepository,
                new RecordJsonMapper(JsonMapper.builder().build()),
                new RecordFieldValidator(),
                new GroupingService(),
                new SortingService(List.of(new AscendingSortStrategy(), new DescendingSortStrategy())));
    }

    // ------------------------------------------------------------ insert

    @Test
    void insertStoresWholeRecordAsJsonAndReturnsId() {
        long id = service.insertRecord(DATASET, Map.of("id", 1, "name", "John Doe"));

        ArgumentCaptor<DatasetRecord> saved = ArgumentCaptor.forClass(DatasetRecord.class);
        verify(recordRepository).save(saved.capture());
        assertThat(id).isEqualTo(1L);
        assertThat(saved.getValue().getDatasetName()).isEqualTo(DATASET);
        assertThat(saved.getValue().getRecordId()).isEqualTo(1L);
        assertThat(saved.getValue().getData()).contains("\"name\":\"John Doe\"");
    }

    @Test
    void insertAcceptsLongIds() {
        assertThat(service.insertRecord(DATASET, Map.of("id", 9_000_000_000L))).isEqualTo(9_000_000_000L);
    }

    @Test
    void insertRejectsMissingId() {
        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("name", "x")))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("id");
        verify(recordRepository, never()).save(any());
    }

    @Test
    void insertRejectsNullId() {
        Map<String, Object> body = new HashMap<>();
        body.put("id", null);

        assertThatThrownBy(() -> service.insertRecord(DATASET, body)).isInstanceOf(InvalidQueryException.class);
    }

    @Test
    void insertRejectsNonIntegerIds() {
        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("id", "1")))
                .isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("id", 1.5)))
                .isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("id", new java.math.BigInteger("99999999999999999999"))))
                .isInstanceOf(InvalidQueryException.class);
        verify(recordRepository, never()).save(any());
    }

    @Test
    void insertRejectsDuplicateIdInSameDataset() {
        when(recordRepository.existsByDatasetNameAndRecordId(DATASET, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("id", 1)))
                .isInstanceOf(DuplicateRecordException.class)
                .hasMessageContaining("1")
                .hasMessageContaining(DATASET);
        verify(recordRepository, never()).save(any());
    }

    @Test
    void insertMapsRaceConditionConstraintViolationToDuplicate() {
        when(recordRepository.save(any())).thenThrow(new DataIntegrityViolationException("uk_dataset_record"));

        assertThatThrownBy(() -> service.insertRecord(DATASET, Map.of("id", 1)))
                .isInstanceOf(DuplicateRecordException.class);
    }

    // ------------------------------------------------------------ query

    @Test
    void queryUnknownDatasetThrowsNotFound() {
        when(recordRepository.findByDatasetNameOrderByPkAsc("nope")).thenReturn(List.of());

        assertThatThrownBy(() -> service.queryDataset("nope", DatasetQuery.of(null, null, null)))
                .isInstanceOf(DatasetNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void queryWithoutOperationsReturnsRecordsInInsertionOrder() {
        stored(entity(1, "{\"id\":1,\"age\":30}"), entity(2, "{\"id\":2,\"age\":25}"));

        QueryResponse response = service.queryDataset(DATASET, DatasetQuery.of(null, null, null));

        assertThat(response.groupedRecords()).isNull();
        assertThat(response.sortedRecords()).isNull();
        assertThat(response.records()).extracting(r -> r.get("id")).containsExactly(1, 2);
    }

    @Test
    void queryGroupsByField() {
        stored(entity(1, "{\"id\":1,\"department\":\"Engineering\"}"),
                entity(2, "{\"id\":2,\"department\":\"Marketing\"}"),
                entity(3, "{\"id\":3,\"department\":\"Engineering\"}"));

        QueryResponse response = service.queryDataset(DATASET, DatasetQuery.of("department", null, null));

        assertThat(response.groupedRecords()).containsOnlyKeys("Engineering", "Marketing");
        assertThat(response.groupedRecords().get("Engineering")).hasSize(2);
        assertThat(response.sortedRecords()).isNull();
    }

    @Test
    void querySortsAscendingAndDescending() {
        stored(entity(1, "{\"id\":1,\"age\":30}"), entity(2, "{\"id\":2,\"age\":25}"), entity(3, "{\"id\":3,\"age\":28}"));

        QueryResponse asc = service.queryDataset(DATASET, DatasetQuery.of(null, "age", "asc"));
        QueryResponse desc = service.queryDataset(DATASET, DatasetQuery.of(null, "age", "desc"));

        assertThat(asc.sortedRecords()).extracting(r -> r.get("age")).containsExactly(25, 28, 30);
        assertThat(desc.sortedRecords()).extracting(r -> r.get("age")).containsExactly(30, 28, 25);
    }

    @Test
    void queryCanGroupAndSortWithinEachGroup() {
        stored(entity(1, "{\"id\":1,\"age\":30,\"department\":\"Engineering\"}"),
                entity(2, "{\"id\":2,\"age\":25,\"department\":\"Engineering\"}"),
                entity(3, "{\"id\":3,\"age\":28,\"department\":\"Marketing\"}"));

        QueryResponse response = service.queryDataset(DATASET, DatasetQuery.of("department", "age", "asc"));

        assertThat(response.groupedRecords().get("Engineering")).extracting(r -> r.get("id")).containsExactly(2, 1);
    }

    @Test
    void sortFieldPresentInOnlySomeGroupsIsNotAnError() {
        stored(entity(1, "{\"id\":1,\"department\":\"A\"}"),
                entity(2, "{\"id\":2,\"department\":\"B\",\"age\":40}"),
                entity(3, "{\"id\":3,\"department\":\"B\",\"age\":20}"));

        QueryResponse response = service.queryDataset(DATASET, DatasetQuery.of("department", "age", "asc"));

        assertThat(response.groupedRecords().get("B")).extracting(r -> r.get("id")).containsExactly(3, 2);
    }

    @Test
    void queryRejectsUnknownGroupByField() {
        stored(entity(1, "{\"id\":1}"));

        assertThatThrownBy(() -> service.queryDataset(DATASET, DatasetQuery.of("salary", null, null)))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("salary");
    }

    @Test
    void queryRejectsUnknownSortField() {
        stored(entity(1, "{\"id\":1}"));

        assertThatThrownBy(() -> service.queryDataset(DATASET, DatasetQuery.of(null, "salary", "asc")))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("salary");
    }

    @Test
    void corruptStoredJsonSurfacesAsServerError() {
        stored(entity(1, "{not json"));

        assertThatThrownBy(() -> service.queryDataset(DATASET, DatasetQuery.of(null, null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------ helpers

    private void stored(DatasetRecord... entities) {
        when(recordRepository.findByDatasetNameOrderByPkAsc(DATASET)).thenReturn(List.of(entities));
    }

    private static DatasetRecord entity(long id, String json) {
        return new DatasetRecord(DATASET, id, json);
    }
}
