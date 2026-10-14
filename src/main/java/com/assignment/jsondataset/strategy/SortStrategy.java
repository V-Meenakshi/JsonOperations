package com.assignment.jsondataset.strategy;

import com.assignment.jsondataset.dto.SortOrder;

import java.util.List;
import java.util.Map;

/** Strategy for sorting records by a field in one particular direction. */
public interface SortStrategy {

    /** The direction this strategy implements; used to register it in  SortingService. */
    SortOrder order();

    /** Sorts records in place by the value of field. */
    void sort(List<Map<String, Object>> records, String field);
}
