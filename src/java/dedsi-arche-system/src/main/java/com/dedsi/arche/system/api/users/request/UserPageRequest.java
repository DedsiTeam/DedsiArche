package com.dedsi.arche.system.api.users.request;

import jakarta.validation.constraints.*;

public record UserPageRequest(
        @Size(max=100) String keyword,
        Boolean enabled,
        @Min(1) @Max(1000000) Integer page,
        @Min(1) @Max(100) Integer size
) {
    public UserPageRequest {
        keyword = keyword == null ? "" : keyword;
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
