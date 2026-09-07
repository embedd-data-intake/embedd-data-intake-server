package com.github.embedd_data_intake.server.dto;

import lombok.*;
import org.springframework.data.domain.Page;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PageDto {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageDto(Page<?> page) {
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}
