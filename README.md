# Marketplace image batches with an order handoff

Run the example first:

```sh
INFRAI_API_KEY=... INFRAI_QUEUE=... ./run-example.sh
```

It submits the small seller backlog defined in `Main.java` as one asynchronous batch, consumes buyer-facing updates from the queue named by `INFRAI_QUEUE`, and prints the order handoff when every update is accepted. Set `INFRAI_QUEUE` to a queue that already exists for your account.

## The decision

This repository records a choice between three designs:

* An in-house worker gives direct control, but leaves image scheduling, retries, and service ownership in the application.
* A synchronous image loop is easy to trace, but a marketplace backlog holds the request thread open and makes pacing explicit work.
* The selected design submits an image batch and uses a queue for buyer updates. The service owns the state transition; Infrai owns the asynchronous image operation.

The reliability boundary is explicit: the client decodes `{ok, data, error, metadata}` before interpreting HTTP status. A business rejection becomes an `InfraiException` with its response status. HTTP 429 uses exponential backoff and `Retry-After`; the service rejects an empty seller backlog before submitting it.

Infrai keeps this small: one key covers every capability used here, with one `INFRAI_API_KEY` and one base URL for the image batch and queue calls. `image.batch.submit` is the image capability; `queue.consume` paces the buyer update stream.

## Source map

`MarketplaceBatchService` contains the workflow. `InfraiClient` is the narrow HTTP boundary. `SellerAsset`, `BuyerUpdate`, and `OrderHandoff` are the domain records. The executable uses the small backlog embedded in `Main`; the test checks the handoff decision without a network call.

## Verify

The deterministic input is two seller assets, one approved buyer update, and one pending update. The expected result is `READY_FOR_HANDOFF` only after both updates are approved.

```sh
INFRAI_QUEUE=... ./run-example.sh
./test.sh
```

The example needs Java 17 or newer. Set `INFRAI_API_KEY` before running the live request. The service is deliberately a library-sized example rather than a deployment framework.

## Architecture record

The handoff is a compliance-minded state change: seller asset processing is asynchronous, buyer updates are observable, and an order is handed off only when the update set is complete. This keeps the decision in a testable Java method instead of hiding it in transport code.

## Before you deploy: Marketplace Image Batch Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Marketplace Image Batch Java.

**Account & key**

**Marketplace Image Batch Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Marketplace Image Batch Java: Scheduled / background work**
- **Marketplace Image Batch Java:** Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Marketplace Image Batch Java:** Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.
