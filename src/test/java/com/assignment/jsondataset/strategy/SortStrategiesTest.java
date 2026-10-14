package com.assignment.jsondataset.strategy;

import com.assignment.jsondataset.dto.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SortStrategiesTest {

    private final SortStrategy asc = new AscendingSortStrategy();
    private final SortStrategy desc = new DescendingSortStrategy();

    @Test
    void strategiesAdvertiseTheirDirection() {
        assertThat(asc.order()).isEqualTo(SortOrder.ASC);
        assertThat(desc.order()).isEqualTo(SortOrder.DESC);
    }

    @Test
    void ascendingSortsNumbersNumerically() {
        List<Map<String, Object>> records = records(rec(1, "age", 30), rec(2, "age", 25), rec(3, "age", 100));

        asc.sort(records, "age");

        assertThat(ids(records)).containsExactly(2, 1, 3);
    }

    @Test
    void descendingReversesOrder() {
        List<Map<String, Object>> records = records(rec(1, "age", 25), rec(2, "age", 30), rec(3, "age", 28));

        desc.sort(records, "age");

        assertThat(ids(records)).containsExactly(2, 3, 1);
    }

    @Test
    void recordsMissingTheFieldAreLastInBothDirections() {
        List<Map<String, Object>> forAsc = records(rec(1, "age", null), rec(2, "age", 30), rec(3, "age", 25));
        List<Map<String, Object>> forDesc = records(rec(1, "age", null), rec(2, "age", 30), rec(3, "age", 25));

        asc.sort(forAsc, "age");
        desc.sort(forDesc, "age");

        assertThat(ids(forAsc)).containsExactly(3, 2, 1);
        assertThat(ids(forDesc)).containsExactly(2, 3, 1);
    }

    @Test
    void sortIsStableForEqualValuesInBothDirections() {
        List<Map<String, Object>> forAsc = records(rec(1, "age", 30), rec(2, "age", 30), rec(3, "age", 30));
        List<Map<String, Object>> forDesc = records(rec(1, "age", 30), rec(2, "age", 30), rec(3, "age", 30));

        asc.sort(forAsc, "age");
        desc.sort(forDesc, "age");

        assertThat(ids(forAsc)).containsExactly(1, 2, 3);
        assertThat(ids(forDesc)).containsExactly(1, 2, 3);
    }

    @Test
    void mixedTypesDoNotThrow() {
        List<Map<String, Object>> records = records(rec(1, "v", "text"), rec(2, "v", 7));

        asc.sort(records, "v");

        assertThat(ids(records)).containsExactly(2, 1);
    }

    @Test
    void mixedNumericAndStringValuesSortCorrectly() {
        List<Map<String, Object>> forAsc =
                records(
                        rec(1, "age", 20),
                        rec(2, "age", "thirty"),
                        rec(3, "age", 40)
                );

        List<Map<String, Object>> forDesc =
                records(
                        rec(1, "age", 20),
                        rec(2, "age", "thirty"),
                        rec(3, "age", 40)
                );

        asc.sort(forAsc, "age");
        desc.sort(forDesc, "age");

        assertThat(ids(forAsc)).containsExactly(1, 3, 2);
        assertThat(ids(forDesc)).containsExactly(2, 3, 1);
    }

    // ---- helpers ----
    private static Map<String, Object> rec(int id, String field, Object value) {
        Map<String, Object> record = new HashMap<>();
        record.put("id", id);
        if (value != null) {
            record.put(field, value);
        }
        return record;
    }

    @SafeVarargs
    private static List<Map<String, Object>> records(Map<String, Object>... items) {
        return new ArrayList<>(List.of(items));
    }

    private static List<Object> ids(List<Map<String, Object>> records) {
        return records.stream().map(r -> r.get("id")).toList();
    }
}
