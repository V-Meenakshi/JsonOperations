package com.assignment.jsondataset.service;

import com.assignment.jsondataset.dto.DatasetQuery;
import com.assignment.jsondataset.dto.QueryResponse;
import com.assignment.jsondataset.entity.DatasetRecord;
import com.assignment.jsondataset.exception.DatasetNotFoundException;
import com.assignment.jsondataset.exception.DuplicateRecordException;
import com.assignment.jsondataset.exception.InvalidQueryException;
import com.assignment.jsondataset.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private static final String ID_FIELD = "id";

    private final RecordRepository recordRepository;
    private final RecordJsonMapper recordJsonMapper;
    private final RecordFieldValidator fieldValidator;
    private final GroupingService groupingService;
    private final SortingService sortingService;

    @Override
    public long insertRecord(String datasetName, Map<String, Object> recordBody) {
        long recordId = extractRecordId(recordBody);

        if (recordRepository.existsByDatasetNameAndRecordId(datasetName, recordId)) {
            throw duplicate(datasetName, recordId);
        }

        DatasetRecord entity = new DatasetRecord(datasetName, recordId, recordJsonMapper.toJson(recordBody));
        try {
            recordRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent inserts can both pass the exists-check; the unique constraint is the real guard.
            throw duplicate(datasetName, recordId);
        }
        return recordId;
    }

    @Override
    @Transactional(readOnly = true)
    public QueryResponse queryDataset(String datasetName, DatasetQuery query) {
        List<DatasetRecord> entities = recordRepository.findByDatasetNameOrderByPkAsc(datasetName);
        if (entities.isEmpty()) {
            throw new DatasetNotFoundException("Dataset '" + datasetName + "' does not exist.");
        }

        List<Map<String, Object>> records = new ArrayList<>(entities.size());
        for (DatasetRecord entity : entities) {
            records.add(recordJsonMapper.toMap(entity));
        }

        // Validate everything up-front so a bad request never does partial work.
        if (query.hasGroupBy()) {
            fieldValidator.requireFieldPresent(records, query.groupBy(), "Group-by");
        }
        if (query.hasSortBy()) {
            fieldValidator.requireFieldPresent(records, query.sortBy(), "Sort");
        }

        if (query.hasGroupBy()) {
            Map<String, List<Map<String, Object>>> groups = groupingService.groupRecords(records, query.groupBy());
            if (query.hasSortBy()) {
                groups.values().forEach(group -> sortingService.sortRecords(group, query.sortBy(), query.order()));
            }
            return QueryResponse.grouped(groups);
        }
        if (query.hasSortBy()) {
            sortingService.sortRecords(records, query.sortBy(), query.order());
            return QueryResponse.sorted(records);
        }
        return QueryResponse.plain(records);
    }

    /** The id must be present and a JSON integer (not null, string, decimal or out of long range). */
    private long extractRecordId(Map<String, Object> recordBody) {
        Object id = recordBody.get(ID_FIELD);
        if (id == null) {
            throw new InvalidQueryException("Record is missing the mandatory '" + ID_FIELD + "' field.");
        }
        if (id instanceof Integer || id instanceof Long) {
            return ((Number) id).longValue();
        }
        throw new InvalidQueryException("Field '" + ID_FIELD + "' must be an integer number.");
    }

    private static DuplicateRecordException duplicate(String datasetName, long recordId) {
        return new DuplicateRecordException(
                "Record with id " + recordId + " already exists in dataset '" + datasetName + "'.");
    }
}