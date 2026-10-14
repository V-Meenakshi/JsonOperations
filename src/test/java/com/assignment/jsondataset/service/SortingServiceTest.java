package com.assignment.jsondataset.service;

import com.assignment.jsondataset.dto.SortOrder;
import com.assignment.jsondataset.strategy.AscendingSortStrategy;
import com.assignment.jsondataset.strategy.DescendingSortStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortingServiceTest {

    private final SortingService sortingService =
            new SortingService(List.of(new AscendingSortStrategy(), new DescendingSortStrategy()));

    @Test
    void delegatesToAscendingStrategy() {
        List<Map<String, Object>> records = sample();

        sortingService.sortRecords(records, "age", SortOrder.ASC);

        assertThat(records).extracting(r -> r.get("age")).containsExactly(25, 28, 30);
    }

    @Test
    void delegatesToDescendingStrategy() {
        List<Map<String, Object>> records = sample();

        sortingService.sortRecords(records, "age", SortOrder.DESC);

        assertThat(records).extracting(r -> r.get("age")).containsExactly(30, 28, 25);
    }

    @Test
    void failsClearlyWhenNoStrategyIsRegisteredForTheOrder() {
        SortingService onlyAsc = new SortingService(List.of(new AscendingSortStrategy()));

        assertThatThrownBy(() -> onlyAsc.sortRecords(sample(), "age", SortOrder.DESC))
                .isInstanceOf(IllegalStateException.class);
    }

    private static List<Map<String, Object>> sample() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int[] row : new int[][] {{1, 30}, {2, 25}, {3, 28}}) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", row[0]);
            record.put("age", row[1]);
            records.add(record);
        }
        return records;
    }
}
