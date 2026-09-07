package com.github.embedd_data_intake.server.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PageDto {
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
}
