package com.drep.analytics.test;

import com.drep.common.security.ApiKeyScope;
import com.drep.common.security.AuthenticatedPrincipal;
import com.drep.common.security.Permission;
import com.drep.common.security.RbacMatrix;
import com.drep.common.security.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RbacMatrixTest {

    @Test
    void analystCanReadButNotIngest() {
        AuthenticatedPrincipal analyst = new AuthenticatedPrincipal(
                UUID.randomUUID(), "tenant-a", Role.ANALYST, ApiKeyScope.READ, false);
        assertTrue(RbacMatrix.isAllowed(analyst, Permission.READ_ANALYTICS));
        assertFalse(RbacMatrix.isAllowed(analyst, Permission.INGEST_EVENTS));
    }

    @Test
    void ingestScopeBlocksAdminOperations() {
        AuthenticatedPrincipal engineer = new AuthenticatedPrincipal(
                UUID.randomUUID(), "tenant-a", Role.ENGINEER, ApiKeyScope.INGEST, false);
        assertTrue(RbacMatrix.isAllowed(engineer, Permission.INGEST_EVENTS));
        assertFalse(RbacMatrix.isAllowed(engineer, Permission.ADMIN_DLQ));
    }

    @Test
    void platformAdminHasAllPermissions() {
        AuthenticatedPrincipal admin = new AuthenticatedPrincipal(
                UUID.randomUUID(), AuthenticatedPrincipal.PLATFORM_TENANT, Role.ADMIN, ApiKeyScope.ADMIN, true);
        assertTrue(RbacMatrix.isAllowed(admin, Permission.ADMIN_API_KEYS));
        assertTrue(RbacMatrix.isAllowed(admin, Permission.ADMIN_TENANT_PROVISION));
    }
}
