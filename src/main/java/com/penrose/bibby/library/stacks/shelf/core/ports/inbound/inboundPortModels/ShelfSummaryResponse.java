package com.penrose.bibby.library.stacks.shelf.core.ports.inbound.inboundPortModels;

public record ShelfSummaryResponse(Long shelfId, String label, long bookCount) {}
