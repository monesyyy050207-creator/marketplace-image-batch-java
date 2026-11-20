package com.example.marketplace;

public record BuyerUpdate(String orderId, String assetId, boolean approved) {}
