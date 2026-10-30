package com.dedsi.arche.system.api.users.request;
import jakarta.validation.constraints.*;
public record ManageUserRequest(@NotBlank @Size(max=100) String displayName, @NotNull Boolean enabled, @NotNull @PositiveOrZero Long version, @Size(max=16) String phone, @Size(max=254) String email) {
    public ManageUserRequest(String displayName, Boolean enabled, Long version) {
        this(displayName, enabled, version, null, null);
    }
}
