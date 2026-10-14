package com.assignment.jsondataset.repository;

import com.assignment.jsondataset.entity.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordRepository extends JpaRepository<DatasetRecord, Long> {

    /** All records of one dataset, in insertion order. Empty if the dataset does not exist. */
    List<DatasetRecord> findByDatasetNameOrderByPkAsc(String datasetName);

    /** Used to reject duplicate ids (409) before attempting the insert. */
    boolean existsByDatasetNameAndRecordId(String datasetName, Long recordId);
}
