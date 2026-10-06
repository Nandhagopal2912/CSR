package com.supportrouter.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    CUSTOMER(EnumSet.of(Permission.CREATE_TICKET, Permission.VIEW_OWN_TICKETS)),
    AGENT(EnumSet.of(Permission.VIEW_ASSIGNED_TICKETS, Permission.START_TICKET, Permission.RESOLVE_TICKET)),
    SUPERVISOR(EnumSet.of(Permission.CREATE_TICKET, Permission.VIEW_ALL_TICKETS, Permission.ASSIGN_TICKET,
            Permission.START_TICKET, Permission.RESOLVE_TICKET, Permission.CLOSE_TICKET));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public boolean allows(Permission permission) {
        return permissions.contains(permission);
    }

    public Set<Permission> getPermissions() {
        return EnumSet.copyOf(permissions);
    }
}
