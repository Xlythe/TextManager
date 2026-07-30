# SMS refactor plan

## Scope

The core SMS path includes outgoing request validation, message persistence, SMS
segmentation and dispatch, sent/delivery result aggregation, background scheduling,
and observer notification. MMS transport is left behavior-compatible except where it
shares scheduling or `Text` serialization with SMS.

The target is at least 90% line coverage for the extracted, platform-independent SMS
core. Android adapters are covered with focused Robolectric tests where platform
behavior matters.

## Audit findings

1. The project targets API 35 and uses Android Gradle Plugin 8.7.2. API 37 requires
   Android Gradle Plugin 9.1.1 or newer and Gradle 9.3.1 or newer.
2. The library has no local unit-test source set, coverage report, or coverage gate.
3. `SendService` mixes persistence, recipient lookup, segmentation, platform dispatch,
   callback creation, and status mutation, which makes failures difficult to isolate
   and test.
4. Outgoing SMS requests are not validated before persistence. Missing recipients,
   unknown contacts, and empty bodies can crash or leave rows permanently pending.
5. Multipart SMS creates one one-shot sent callback and one one-shot delivery callback
   for all parts. Each part needs a distinct callback.
6. Sent and delivery callbacks update the same status independently. An early success
   can hide a later part failure, and a sent callback currently marks a message
   delivered before delivery is confirmed.
7. `Text` parcel restoration reads prepared MMS/SMS bytes into a null array.
8. `TextManager` iterates a mutable `HashSet` of observers while callers may register
   or unregister from another thread.
9. `IntentService` plus `Context.startService()` is not a reliable background
   scheduling mechanism on modern Android.
10. `SmsManager.getDefault()` does not give callers control over the subscription on
    multi-SIM devices.

## Ordered, commit-sized implementation

1. Modernize the build for API 37 with Android Gradle Plugin 9.1.1 and Gradle 9.3.1.
2. Add library unit tests, JaCoCo reporting, and a 90% coverage gate for the core SMS
   package.
3. Extract a dependency-injected SMS dispatch coordinator without changing behavior.
4. Reject invalid outgoing SMS requests before persistence and report failures
   deterministically.
5. Add thread-safe, idempotent multipart sent/delivery aggregation and distinct
   per-part callbacks.
6. Fix `Text` parcel restoration so deferred work can safely round-trip payload bytes.
7. Replace direct background service starts with persistent WorkManager scheduling
   while preserving the existing public send API.
8. Make observer registration and notification safe under concurrent access.
9. Add an optional subscription ID to outgoing texts and use a subscription-specific
   `SmsManager` when supplied.
10. Run unit tests, Robolectric tests, lint/compilation checks, and the core coverage
    gate; document any environment-only verification limits.

Each numbered implementation item is committed separately. If an item exposes an
independent defect, that defect receives its own commit rather than being folded into
the current change.
