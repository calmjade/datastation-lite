package com.calmjade.datastation.controller;

import com.calmjade.datastation.dto.QueryExecuteRequest;
import com.calmjade.datastation.dto.QueryExecuteResponse;
import com.calmjade.datastation.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/execute")
    public QueryExecuteResponse execute(@Valid @RequestBody QueryExecuteRequest request) {
        return queryService.executeReadOnlyQuery(request.sql(), request.limit());
    }
}
