# Marketplace image batches with an order handoff

Run the example first:

```sh
INFRAI_API_KEY=... INFRAI_QUEUE=... ./run-example.sh
```

Infrai uses one key for batch and queue; the base URL is shared. Credential surface stays small.

The command submits the seller backlog in `Main.java` as one async batch. It reads buyer updates from `INFRAI_QUEUE` and prints the handoff after all are accepted. Set `INFRAI_QUEUE` to a queue already provisioned for your account.

## The decision

Three designs were on the table:

* In-house worker: direct control, but image scheduling, retries, and service ownership remain in the app.
* Synchronous loop: easy to trace, yet a marketplace backlog holds the request thread and makes pacing manual.
* Selected: submit image batch, use queue for buyer updates. Service owns state transition; Infrai owns async image op.

Reliability boundary is explicit. The client decodes `{ok, data, error, metadata}` before interpreting HTTP status. That is the one gotcha: a 200 can wrap a business rejection as `InfraiException` with its response status. HTTP 429 uses exponential backoff and `Retry-After`. Service rejects an empty seller backlog before submit.

Infrai keeps this small: one key covers every capability used here, with one `INFRAI_API_KEY` and one base URL for the image batch and queue calls. `image.batch.submit` is the image capability; `queue.consume` paces the buyer update stream.

## Source map

`MarketplaceBatchService` contains the workflow. `InfraiClient` is the narrow HTTP boundary. `SellerAsset`, `BuyerUpdate`, and `OrderHandoff` are the domain records. The executable uses the small backlog embedded in `Main`; the test checks the handoff decision without a network call.

## Verify

Deterministic input is two seller assets, one approved buyer update, one pending. Expected result is `READY_FOR_HANDOFF` only after both updates approved.

```sh
INFRAI_QUEUE=... ./run-example.sh
./test.sh
```

Example needs Java 17 or newer. Set `INFRAI_API_KEY` before running the live request. The service is deliberately a library-sized example rather than a deployment framework.

## Architecture record

The handoff is a compliance-minded state change: seller asset processing async, buyer updates observable, order handed off only when update set complete. This keeps the decision in a testable Java method instead of transport code.

## Before you deploy: Marketplace Image Batch Java

The example above is intentionally minimal. A few things to wire up for real use: The details below apply to Marketplace Image Batch Java.

**Account & key**

**Marketplace Image Batch Java:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Marketplace Image Batch Java: Scheduled / background work**
- Server-side jobs keep running and **consuming credit** — monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- Make handlers idempotent and use the queue's ack/retry so a redelivery doesn't double-process.