package com.example.marketplace;

import java.util.List;

public final class MarketplaceBatchServiceTest {
    public static void main(String[] args) {
        MarketplaceBatchService service = new MarketplaceBatchService(null);
        List<BuyerUpdate> updates = List.of(new BuyerUpdate("order-501", "shoe-101", true), new BuyerUpdate("order-501", "shoe-102", true));
        OrderHandoff result = service.decideHandoff("order-501", updates);
        if (!"READY_FOR_HANDOFF".equals(result.status())) throw new AssertionError(result);
        System.out.println("handoff decision: " + result.status());
    }
}
