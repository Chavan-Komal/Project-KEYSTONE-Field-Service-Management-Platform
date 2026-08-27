package com.zidio.keystone.controller;

import com.zidio.keystone.dto.DashboardSummaryResponse;
import com.zidio.keystone.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return reportService.getSummary();
    }
}
