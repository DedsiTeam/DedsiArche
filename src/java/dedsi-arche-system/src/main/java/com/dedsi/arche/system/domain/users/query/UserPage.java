package com.dedsi.arche.system.domain.users.query;
import java.util.List;
public record UserPage(List<ManagedUserDto> items, long total, int page, int size) {}
