package com.mint.search.content;

import com.mint.search.search.dto.SearchItemDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentPreviewRequest {
    private SearchItemDto item;
}
