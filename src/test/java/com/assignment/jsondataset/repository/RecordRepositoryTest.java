package com.assignment.jsondataset.repository;

import com.assignment.jsondataset.entity.DatasetRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RecordRepositoryTest {

    @Autowired
    private RecordRepository repository;

    @Test
    void findsRecordsOfOneDatasetInInsertionOrder() {
        repository.save(new DatasetRecord("a", 2L, "{\"id\":2}"));
        repository.save(new DatasetRecord("b", 1L, "{\"id\":1}"));
        repository.save(new DatasetRecord("a", 1L, "{\"id\":1}"));

        List<DatasetRecord> result = repository.findByDatasetNameOrderByPkAsc("a");

        assertThat(result).extracting(DatasetRecord::getRecordId).containsExactly(2L, 1L);
    }

    @Test
    void unknownDatasetYieldsEmptyList() {
        assertThat(repository.findByDatasetNameOrderByPkAsc("missing")).isEmpty();
    }

    @Test
    void existsByDatasetNameAndRecordIdIsScopedToTheDataset() {
        repository.save(new DatasetRecord("a", 1L, "{\"id\":1}"));

        assertThat(repository.existsByDatasetNameAndRecordId("a", 1L)).isTrue();
        assertThat(repository.existsByDatasetNameAndRecordId("a", 2L)).isFalse();
        assertThat(repository.existsByDatasetNameAndRecordId("b", 1L)).isFalse();
    }

    @Test
    void sameIdIsAllowedInDifferentDatasets() {
        repository.saveAndFlush(new DatasetRecord("a", 1L, "{\"id\":1}"));

        repository.saveAndFlush(new DatasetRecord("b", 1L, "{\"id\":1}"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void databaseRejectsDuplicateIdInSameDataset() {
        repository.saveAndFlush(new DatasetRecord("a", 1L, "{\"id\":1}"));

        assertThatThrownBy(() -> repository.saveAndFlush(new DatasetRecord("a", 1L, "{\"id\":1}")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
