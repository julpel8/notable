# Licenses and provenance

## This repository

This project is a fork of [Ethran/notable](https://github.com/Ethran/notable) and is licensed under the
**GNU General Public License v3.0** (see [LICENSE](LICENSE)). All modifications and additions in this fork
are published under the same license, as required by the GPL.

## Lineage

- [olup/notable](https://github.com/olup/notable) — original project (GPL-3.0).
- [Ethran/notable](https://github.com/Ethran/notable) — maintained continuation of olup/notable (GPL-3.0).
  This repository is forked from it and tracks it for upstream merges (package/applicationId intentionally
  kept as `com.ethran.notable` to keep rebases cheap).

## Code ported from other GPL-3.0 projects

### jdkruzr/aragonite

[jdkruzr/aragonite](https://github.com/jdkruzr/aragonite) is itself a GPL-3.0 fork of Ethran/notable.
The following files were ported from it (GPL-3.0 → GPL-3.0, compatible):

- `app/src/main/aidl/com/onyx/android/sdk/hwr/service/*.aidl` — AIDL declaration of the Boox firmware
  handwriting-recognition binder interface (`com.onyx.android.ksync/.service.KHwrService`). This is a
  clean re-declaration of the service's public binder surface (method signatures and transaction order);
  it contains no Onyx code.
- `app/src/main/java/com/onyx/android/sdk/hwr/service/*.kt` — hand-written `Parcelable` classes matching
  the parcel layout expected by that service.
- `app/src/main/java/com/ethran/notable/io/OnyxHWREngine.kt` — service binding, hand-rolled protobuf
  encoding of strokes, and result parsing for the firmware MyScript recognizer.

Later sessions are expected to port additional Aragonite subsystems (annotation boxes, background
recognition, Markdown export); those ports must be recorded here when they land.

## Visual references (no code)

- [gaborauth/toolsboox-android](https://github.com/gaborauth/toolsboox-android) — used strictly as visual
  inspiration for the daily/planner page layout. **No code was copied from it.**

## Third-party SDKs

- Onyx Boox SDK artifacts (`onyxsdk-pen`, `onyxsdk-device`, `onyxsdk-base`) are proprietary binaries
  consumed as Maven dependencies from `repo.boox.com` (unchanged from upstream Notable). They are not
  redistributed in this repository.

## Obligations summary (GPL-3.0)

- Source must remain available under GPL-3.0 for any distributed binary of this app.
- Keep this provenance file and the top-level `LICENSE` intact.
- Any code mixed into this repository must be GPL-3.0 compatible.
