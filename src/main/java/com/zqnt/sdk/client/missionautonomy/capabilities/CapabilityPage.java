package com.zqnt.sdk.client.missionautonomy.capabilities;

import java.util.List;

/** Immutable page returned by capability list operations. */
public record CapabilityPage<T>(List<T> items, String nextPageToken) {
    public CapabilityPage {
        items = items == null ? List.of() : List.copyOf(items);
        nextPageToken = nextPageToken == null ? "" : nextPageToken;
    }

    public boolean hasNextPage() {
        return !nextPageToken.isBlank();
    }
}
