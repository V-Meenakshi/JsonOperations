package com.assignment.jsondataset.service;

import com.assignment.jsondataset.dto.SortOrder;
import com.assignment.jsondataset.strategy.SortStrategy;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Picks the {@link SortStrategy} matching the requested direction and applies it. */
@Service
public class SortingService {

    private final Map<SortOrder, SortStrategy> strategies = new EnumMap<>(SortOrder.class);

    /** Spring injects every  bean that are available */
    public SortingService(List<SortStrategy> availableStrategies) {
        for (SortStrategy strategy : availableStrategies) {
            strategies.put(strategy.order(), strategy);
        }
    }

    /** Sorts records in place. The list must be mutable. */
    public void sortRecords(List<Map<String, Object>> records, String sortBy, SortOrder order) {
        SortStrategy strategy = strategies.get(order);
        if (strategy == null) {
            throw new IllegalStateException("No sort strategy registered for order " + order);
        }
        strategy.sort(records, sortBy);
    }
}
