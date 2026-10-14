package com.assignment.jsondataset.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GroupingServiceTest {

    private final GroupingService groupingService = new GroupingService();

    @Test
    void groupsRecordsByFieldValueKeepingOrder() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "department", "Engineering"),
                Map.of("id", 2, "department", "Marketing"),
                Map.of("id", 3, "department", "Engineering"));

        Map<String, List<Map<String, Object>>> grouped = groupingService.groupRecords(records, "department");

        assertThat(grouped.keySet()).containsExactly("Engineering", "Marketing");
        assertThat(grouped.get("Engineering")).extracting(r -> r.get("id")).containsExactly(1, 3);
        assertThat(grouped.get("Marketing")).extracting(r -> r.get("id")).containsExactly(2);
    }

    @Test
    void numericValuesBecomeTextKeys() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "age", 30), Map.of("id", 2, "age", 30));

        assertThat(groupingService.groupRecords(records, "age")).containsOnlyKeys("30");
    }

    @Test
    void missingAndNullValuesShareTheNullGroup() {
        Map<String, Object> explicitNull = new HashMap<>();
        explicitNull.put("id", 2);
        explicitNull.put("department", null);
        List<Map<String, Object>> records = List.of(Map.of("id", 1), explicitNull);

        Map<String, List<Map<String, Object>>> grouped = groupingService.groupRecords(records, "department");

        assertThat(grouped).containsOnlyKeys("null");
        assertThat(grouped.get("null")).hasSize(2);
    }

    @Test
    void emptyInputGivesEmptyResult() {
        assertThat(groupingService.groupRecords(List.of(), "department")).isEmpty();
    }
}
