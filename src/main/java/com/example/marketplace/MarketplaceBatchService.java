package com.example.marketplace;

import java.io.IOException;
import java.util.List;

public final class MarketplaceBatchService {
    private final InfraiClient client;
    public MarketplaceBatchService(InfraiClient client) { this.client = client; }

    public String submit(List<SellerAsset> assets) throws IOException, InterruptedException {
        if (assets.isEmpty()) throw new IllegalArgumentException("seller asset backlog is empty");
        StringBuilder items = new StringBuilder("{\"items\":[");
        for (int i = 0; i < assets.size(); i++) {
            if (i > 0) items.append(',');
            SellerAsset asset = assets.get(i);
            items.append("{\"image\":\"").append(asset.image()).append("\"}");
        }
        return client.submitImageBatch(items.append("],\"store\":false}").toString());
    }

    public String consumeBuyerUpdates() throws IOException, InterruptedException {
        String queue = System.getenv("INFRAI_QUEUE");
        if (queue == null || queue.isBlank()) throw new IllegalStateException("INFRAI_QUEUE is required");
        return client.consumeBuyerUpdates("{\"queue\":\"" + queue + "\",\"max_messages\":10,\"visibility_timeout\":30}");
    }

    public OrderHandoff decideHandoff(String orderId, List<BuyerUpdate> updates) {
        boolean complete = !updates.isEmpty() && updates.stream().allMatch(u -> u.approved() && orderId.equals(u.orderId()));
        return new OrderHandoff(orderId, complete ? "READY_FOR_HANDOFF" : "PENDING_BUYER_UPDATE");
    }
}
