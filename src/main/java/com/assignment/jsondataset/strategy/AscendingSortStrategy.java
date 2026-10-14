package com.assignment.jsondataset.strategy;

import com.assignment.jsondataset.dto.SortOrder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Ascending order; records missing the field (or holding null) are placed last. */
@Component
public class AscendingSortStrategy implements SortStrategy {

    private static final Comparator<Object> COMPARATOR = Comparator.nullsLast(FieldValueComparator.INSTANCE);

    @Override
    public SortOrder order() {
        return SortOrder.ASC;
    }

    @Override
    public void sort(List<Map<String, Object>> records, String field) {
        records.sort((a, b) -> COMPARATOR.compare(a.get(field), b.get(field)));
    }
}
