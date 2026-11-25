package com.calmjade.datastation.service;

import com.calmjade.datastation.dto.QueryExecuteResponse;
import com.calmjade.datastation.dto.ReportRequest;
import com.calmjade.datastation.dto.ReportRunResponse;
import com.calmjade.datastation.entity.ReportConfig;
import com.calmjade.datastation.repository.ReportConfigRepository;
import com.calmjade.datastation.security.SqlSecurityValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportService {

    private final ReportConfigRepository reportRepository;
    private final QueryService queryService;
    private final SqlSecurityValidator sqlSecurityValidator;

    public ReportService(
            ReportConfigRepository reportRepository,
            QueryService queryService,
            SqlSecurityValidator sqlSecurityValidator
    ) {
        this.reportRepository = reportRepository;
        this.queryService = queryService;
        this.sqlSecurityValidator = sqlSecurityValidator;
    }

    public List<ReportConfig> findAll() {
        return reportRepository.findAllByOrderByUpdatedAtDescIdDesc();
    }

    public ReportConfig findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到编号为 " + id + " 的报表配置"));
    }

    public ReportConfig create(ReportRequest request) {
        String cleanSql = sqlSecurityValidator.validateAndSanitize(request.sqlText());
        ReportConfig entity = new ReportConfig();
        applyRequest(entity, request, cleanSql);
        entity.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(entity);
    }

    public ReportConfig update(Long id, ReportRequest request) {
        ReportConfig entity = findById(id);
        String cleanSql = sqlSecurityValidator.validateAndSanitize(request.sqlText());
        applyRequest(entity, request, cleanSql);
        entity.setUpdatedAt(LocalDateTime.now());
        return reportRepository.save(entity);
    }

    public void delete(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到编号为 " + id + " 的报表配置");
        }
        reportRepository.deleteById(id);
    }

    public ReportRunResponse runReport(Long id, Integer limit) {
        ReportConfig report = findById(id);
        QueryExecuteResponse queryResult = queryService.executeReadOnlyQuery(
                report.getSqlText(),
                limit != null ? limit : QueryService.MAX_ROW_LIMIT
        );
        return new ReportRunResponse(
                report,
                queryResult.columns(),
                queryResult.rows(),
                queryResult.rowCount(),
                queryResult.executionMs()
        );
    }

    public long countReports() {
        return reportRepository.count();
    }

    private void applyRequest(ReportConfig entity, ReportRequest request, String cleanSql) {
        entity.setName(request.name().trim());
        entity.setCategory(request.category().trim());
        entity.setDescription(request.description() != null ? request.description().trim() : "");
        entity.setSqlText(cleanSql);
        entity.setColumnsJson(request.columnsJson() != null ? request.columnsJson() : "[]");
        entity.setChartConfigJson(request.chartConfigJson() != null ? request.chartConfigJson() : "{}");
        entity.setTransformScript(request.transformScript() != null ? request.transformScript() : "");
    }
}
