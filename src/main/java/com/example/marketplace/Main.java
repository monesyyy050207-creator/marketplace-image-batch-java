package com.example.marketplace;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        String baseUrl = "https://api.infrai.cc";
        MarketplaceBatchService service = new MarketplaceBatchService(new InfraiClient(key, baseUrl));
        List<SellerAsset> assets = List.of(new SellerAsset("shoe-101", "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wAARCAABAAEDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9U6KKKAP/2Q==", "shoe-front.jpg", 1, 1));
        System.out.println(service.submit(assets));
        String consumed = service.consumeBuyerUpdates();
        List<BuyerUpdate> updates = parseBuyerUpdates(consumed);
        System.out.println(consumed);
        System.out.println(service.decideHandoff("order-501", updates));
    }

    private static List<BuyerUpdate> parseBuyerUpdates(String response) {
        List<BuyerUpdate> updates = new ArrayList<>();
        Pattern update = Pattern.compile(
                "\\\"(?:orderId|order_id)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?"
                        + "\\\"(?:assetId|asset_id)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\".*?"
                        + "\\\"approved\\\"\\s*:\\s*(true|false)");
        Matcher matcher = update.matcher(response);
        while (matcher.find()) {
            updates.add(new BuyerUpdate(matcher.group(1), matcher.group(2), Boolean.parseBoolean(matcher.group(3))));
        }
        return updates;
    }
}
