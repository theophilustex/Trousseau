package com.trousseau.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lightweight projection of ClothingItem — contains only display fields, no BLOBs.
 * Used wherever a full ClothingItem load would trigger an OOM due to image data.
 */
@Getter
@RequiredArgsConstructor
public class ClothingItemSummary {
    private final Long id;
    private final String name;
    private final String category;
}
