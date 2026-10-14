package com.assignment.jsondataset.strategy;

import com.assignment.jsondataset.dto.SortOrder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Descending order; records missing the field (or holding null) are still placed last. */
@Component
public class DescendingSortStrategy implements SortStrategy {

    private static final Comparator<Object> COMPARATOR =
            Comparator.nullsLast(FieldValueComparator.INSTANCE.reversed());

    @Override
    public SortOrder order() {
        return SortOrder.DESC;
    }

    @Override
    public void sort(List<Map<String, Object>> records, String field) {
        records.sort((a, b) -> COMPARATOR.compare(a.get(field), b.get(field)));
    }
}
