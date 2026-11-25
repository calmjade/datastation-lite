package com.calmjade.datastation.repository;

import com.calmjade.datastation.entity.ReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportConfigRepository extends JpaRepository<ReportConfig, Long> {

    List<ReportConfig> findAllByOrderByUpdatedAtDescIdDesc();
}
