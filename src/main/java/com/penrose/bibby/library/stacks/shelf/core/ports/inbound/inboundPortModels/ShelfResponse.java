package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels;

import java.util.List;

public record ShelfResponse(
    Long id,
    int shelfPosition,
    String shelfLabel,
    int bookCapacity,
    List<Long> bookIds,
    Long bookcaseId) {}
