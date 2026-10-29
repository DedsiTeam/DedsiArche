package com.dedsi.arche.system.api.users.request;
import jakarta.validation.constraints.*;
public record ManageUserRequest(@NotBlank @Size(max=100) String displayName, @NotNull Boolean enabled, @NotNull @PositiveOrZero Long version) {}
