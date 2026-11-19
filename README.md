# AutoTally Pay

A digital khata for Android shopkeepers in Pakistan. Reads Easypaisa,
JazzCash and bank payment SMS (Meezan, Faysal, Bank AL Habib), matches
them to customer bills, and shows who still owes how much.

## Highlights

- **Fake-SMS protection** — a "payment received" SMS from an ordinary
  mobile number (03xx / +92 3xx) is never counted as paid. It is stored
  flagged and shown in red with the reason. Only known short codes and
  sender IDs count as verified.
- **Fully offline** — no INTERNET permission in any flavor. Data stays
  on the shopkeeper's phone, with a password-encrypted backup
  (AES-256-GCM) they control.
- **"Did the money come?"** — type an amount, get a yes/no in one
  second, with the matching Trx ID.
- **Name memory** — assign a bank payment to a customer once; their
  next payments match by themselves.
- **Lite flavor** — no SMS permission at all; the shopkeeper shares
  the SMS text into the app instead.
- **Bilingual** — English + Urdu (Noto Nastaliq Urdu, bundled), big
  buttons, and a red-to-green animated moment when a bill is fully paid.

## Under the hood

Kotlin, Jetpack Compose, Room, WorkManager. Money is stored in paisa
(Long), never floats. One payment splits across bills oldest-first
(FIFO), and "paid" is never stored — always calculated from
allocations. SMS formats were built from typical real Pakistani bank
message patterns (all test data is masked/fictional).

## Flavors

| Flavor | SMS permission | How payments arrive |
|--------|----------------|---------------------|
| `full` | RECEIVE_SMS + READ_SMS | Auto-read via BroadcastReceiver |
| `lite` | none | Share intent / paste |

## Tests

62 JUnit tests cover every SMS parser (including fake-sender
rejection), FIFO allocation (including partial splits), paisa math,
paid-calculation, name-memory matching, and the amount checker.

```sh
./gradlew testDebugUnitTest
```

## Build

After cloning, restore the binary assets first (font, launcher icons,
Gradle wrapper jar — stored as base64 text under `tools/assets-b64/`):

```sh
sh tools/decode-assets.sh
./gradlew testDebugUnitTest
./gradlew assembleFullRelease assembleLiteRelease
```

Release signing reads `keystore.properties` at the project root
(gitignored). GitHub Actions runs the 62 tests, then builds both
release APKs.

## Signing key

One release keystore, created for v1.0.0 and reused for all future
versions. Backed up under `keystore-backup/` (gitignored) — keystore
file + passwords file. Never committed, never shared.
