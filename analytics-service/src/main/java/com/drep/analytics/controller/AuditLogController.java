package com.drep.analytics.controller;

import com.drep.analytics.auth.AuditService;
import com.drep.analytics.auth.SecuritySupport;
import com.drep.common.security.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
public class AuditLogController {

    private final AuditService auditService;
    private final SecuritySupport securitySupport;

    public AuditLogController(AuditService auditService, SecuritySupport securitySupport) {
        this.auditService = auditService;
        this.securitySupport = securitySupport;
    }

    @GetMapping
    public List<AuditService.AuditEntry> list(@RequestParam(defaultValue = "100") int limit,
                                              HttpServletRequest request) {
        securitySupport.requirePermission(request, Permission.ADMIN_AUDIT_READ);
        return auditService.recent(Math.min(limit, 500));
    }
}
