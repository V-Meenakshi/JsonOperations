package com.assignment.jsondataset.service;

import com.assignment.jsondataset.dto.DatasetQuery;
import com.assignment.jsondataset.dto.QueryResponse;

import java.util.Map;

public interface DatasetService {

    /**
     * Stores a JSON record in the given dataset (created implicitly on first insert).
     */
    long insertRecord(String datasetName, Map<String, Object> recordBody);

    /**
     * Returns the dataset's records, optionally grouped and/or sorted.
     */
    QueryResponse queryDataset(String datasetName, DatasetQuery query);
}
