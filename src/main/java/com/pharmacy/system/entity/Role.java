package com.pharmacy.system.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum Role {

    ROLE_ADMIN (
            Set.of(
                    "admin:read",
                    "admin:write",
                    "admin:update",
                    "admin:delete",
                    "pharmacy:read",
                    "pharmacy:write",
                    "inventory:read",
                    "inventory:write"
            )
    ),
    ROLE_STORE_MANAGER(
            Set.of(
                    "inventory:read",
                    "inventory:write",
                    "inventory:update",
                    "pharmacy:read"
            )
    ),
    ROLE_PHARMACIST(
            Set.of(
                    "pharmacy:read",
                    "pharmacy:write",
                    "inventory:read"
            )
    ),
    ROLE_CASHIER(
            Set.of(
                    "pharmacy:read",
                    "billing:create"
            )
    );

    private final Set<String> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = new ArrayList<SimpleGrantedAuthority>(
                permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
        authorities.add(new SimpleGrantedAuthority(this.name()));
        return  authorities;
    }

}
