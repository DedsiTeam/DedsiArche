package com.dedsi.arche.system.api.users.request;

import jakarta.validation.constraints.*;

/** 用户分页筛选；联系方式支持部分内容匹配，空条件不参与筛选。 */
public record UserPageRequest(
        @Size(max = 100) String keyword,
        Boolean enabled,
        @Min(1) @Max(1000000) Integer page,
        @Min(1) @Max(100) Integer size,
        @Size(max = 16) String phone,
        @Size(max = 254) String email
) {
    public UserPageRequest {
        keyword = keyword == null ? "" : keyword.trim();
        phone = phone == null ? "" : phone.trim();
        email = email == null ? "" : email.trim();
        page = page == null ? 1 : page;
        size = size == null ? 10 : size;
    }
}
