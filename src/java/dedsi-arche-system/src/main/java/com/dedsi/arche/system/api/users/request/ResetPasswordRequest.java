package com.dedsi.arche.system.api.users.request;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(min=12,max=64) String password,
                                   @NotNull @PositiveOrZero Long version) {}
