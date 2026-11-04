package com.back.atlas.dto;

import lombok.Getter;

import java.util.List;

/** One page of a list. A plain class, not a record: the app serializes with Gson. */
@Getter
public class PageResponse<T> {
    private final int page;
    private final int size;
    private final int total;
    private final int totalPages;
    private final List<T> data;
    private final boolean hasNext;
    private final boolean hasPrevious;

    private PageResponse(int page, int size, List<T> all) {
        this.page = page;
        this.size = size;
        this.total = all.size();
        this.totalPages = (int) Math.ceil((double) total / size);
        // long: (page - 1) * size can overflow an int for absurd page numbers.
        int from = (int) Math.min((long) (page - 1) * size, total);
        int to = Math.min(from + size, total);
        this.data = List.copyOf(all.subList(from, to));
        this.hasNext = to < total;
        this.hasPrevious = from > 0;
    }

    /** Page numbers start at 1. */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        return new PageResponse<>(page, size, all);
    }
}
