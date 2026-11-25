package com.calmjade.datastation.controller;

import com.calmjade.datastation.dto.TableMetaDto;
import com.calmjade.datastation.service.SchemaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping("/tables")
    public List<TableMetaDto> listTables() {
        return schemaService.getAllTableMetadata();
    }
}
