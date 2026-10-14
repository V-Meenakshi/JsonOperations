package com.assignment.jsondataset.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupingService {

    /** Group key used for records where the field is missing or null. */
    static final String MISSING_VALUE_KEY = "null";

    /**
     * Groups records by the text form of {@code field}. Groups appear in order of first
     * occurrence and records keep their relative order inside each group.
     */
    public Map<String, List<Map<String, Object>>> groupRecords(List<Map<String, Object>> records, String field) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            Object value = record.get(field);
            String key = value == null ? MISSING_VALUE_KEY : value.toString();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }
        return groups;
    }
}
