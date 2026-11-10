# Alpha Desktop 0.6.2 — quiet original sounds, no leaf stripping

## Driver-timeout warning

The user reported an AMD graphics-driver timeout with 0.6.1. Its cause is unresolved; this release is **not a confirmed driver fix**. Do not repeatedly retry after a driver reset. Direct play now displays an explicit warning before launching. No driver, registry or Windows timeout settings are changed.

Fog modifications are disabled. The offline patcher no longer inserts any fog/render-call hooks. No new visual effects are added while the graphics failure remains unresolved.

## Atmosphere changes

- Rare quiet positional footsteps, using only the original `step.grass`, `step.stone` and `step.gravel` sounds through Alpha's existing world sound engine. No new recordings, generated audio, loud stingers or separate audio player.
- Random 4–9-minute intervals measured in active game ticks. Menus/paused ticks do not advance the timer. Sounds originate about six blocks behind/to the side; no entity is spawned. Normal volume/mute controls apply. Missing recordings cannot play.
- Leaf stripping is completely disabled. Existing trees are never deliberately stripped by the new hooks. No new bare-tree generation is implemented. Previously saved leaf loss cannot be automatically repaired.
- Rare corridor/sand/gravel effects remain. No stalking entity, visual apparition, jumpscare, chat, fake crash or desktop effect. Ambiguity comes from distant familiar sounds and environmental details, not system instability.

## Retained compatibility

Java 8 legacy sorting setting and local original-sound registration fix remain. Original JAR, Recovery worlds and launcher profiles remain untouched. Select **Play patched Alpha** with the full Windows distribution. Local single-player only; matching installed a1.0.16 JSON/libraries/natives still required.

Automated tests cover unchanged leaves/fog, sparse sound scheduling, original-only sound keys, patch structure and launcher safety. The exact supplied client was patched locally and stack/local types verified for 99 methods in modified/added classes. These checks are not gameplay, audio-playback or AMD-driver verification. Minecraft code/media and patched game binaries are not distributed.
