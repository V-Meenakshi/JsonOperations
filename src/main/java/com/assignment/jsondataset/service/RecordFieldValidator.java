package com.assignment.jsondataset.service;

import com.assignment.jsondataset.exception.InvalidQueryException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** Checks that a field named in a query actually occurs in the dataset. */
@Component
public class RecordFieldValidator {

  
    public void requireFieldPresent(List<Map<String, Object>> records, String field, String role) {
        boolean present = records.stream().anyMatch(record -> record.containsKey(field));
        if (!present) {
            throw new InvalidQueryException(role + " field '" + field + "' does not exist in the dataset.");
        }
    }
}
