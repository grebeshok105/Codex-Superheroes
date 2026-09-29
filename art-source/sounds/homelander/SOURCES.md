# Homelander sound sources

Provenance for the Homelander flight/laser runtime SFX in
`src/main/resources/assets/superheroes/sounds/homelander/` (replaced 2026-09-29,
previously synthesized placeholder tones).

## Licenses

- **Mixkit** — [Mixkit Stock Sound Free License](https://mixkit.co/license/#sfxFree):
  free for commercial and non-commercial use, no attribution required.
  Raw mp3s kept next to this file as `mixkit-<id>-<slug>.mp3`.
- **Pixabay (user-provided)** — [Pixabay Content License](https://pixabay.com/service/license-summary/):
  free to use for commercial/non-commercial purposes, no attribution required.
  Downloaded earlier by the user (`cUsersstravvberyDownloads*.mp3`).

## Per-file mapping

| Runtime file | Composition (ffmpeg/numpy mix, OGG Vorbis q4, 44.1 kHz) |
| --- | --- |
| `flight_takeoff.ogg` | mixkit-1714 rocket whoosh (0.30–1.80 s) + ground-impact head layer −9 dB; fades |
| `flight_loop.ogg` | mixkit-1267 mountain wind (5.0–12.5 s, +3.5 dB >1.5 kHz) + mixkit-894 wind tunnel −7 dB; 6.0 s seamless loop, 1.5 s equal-power crossfade |
| `flight_boost.ogg` | pixabay thunderclap (0.30–2.35 s) + mixkit-2599 shockwave tail at +0.50 s −7 dB; fades |
| `flight_land.ogg` | pixabay ground-impact (0–1.44 s) + mixkit-1703 rocks debris −11 dB; fades |
| `laser_charge.ogg` | mixkit-2600 static power-up (0.05–0.95 s); fades |
| `laser_loop.ogg` | mixkit-2745 drone hum (30.0–36.5 s) + mixkit-332 insect buzz looped −8 dB + mixkit-2825 laser shimmer looped −13 dB; 5.2 s seamless loop, 1.3 s equal-power crossfade |
| `laser_release.ogg` | mixkit-1946 laser game-over (0–1.15 s); fades |

## Source URLs

| Asset | Source |
| --- | --- |
| mixkit-1714 Fast rocket whoosh | https://mixkit.co/free-sound-effects/discover/fast-rocket-whoosh-1714/ |
| mixkit-894 Horror sci-fi wind tunnel | https://mixkit.co/free-sound-effects/discover/horror-sci-fi-wind-tunnel-894/ |
| mixkit-1267 Wind in the top of the mountain | https://mixkit.co/free-sound-effects/discover/wind-in-the-top-of-the-mountain-1267/ |
| mixkit-2599 Heavy electric shockwave impact | https://mixkit.co/free-sound-effects/discover/heavy-electric-shockwave-impact-2599/ |
| mixkit-1703 Explosion with rocks debris | https://mixkit.co/free-sound-effects/discover/explosion-with-rocks-debris-1703/ |
| mixkit-2600 Electricity static power up | https://mixkit.co/free-sound-effects/discover/electricity-static-power-up-2600/ |
| mixkit-2745 Low drone engine hum | https://mixkit.co/free-sound-effects/discover/low-drone-engine-hum-2745/ |
| mixkit-332 Robotic insect buzz | https://mixkit.co/free-sound-effects/discover/robotic-insect-buzz-332/ |
| mixkit-2825 Sci-fi laser in space sound | https://mixkit.co/free-sound-effects/discover/sci-fi-laser-in-space-sound-2825/ |
| mixkit-1946 Laser game over | https://mixkit.co/free-sound-effects/discover/laser-game-over-1946/ |
| pixabay pwlpl-thunderclap-377255 (user download) | https://pixabay.com/sound-effects/thunderclap-377255/ |
| pixabay universfield-ground-impact-352053 (user download) | https://pixabay.com/sound-effects/ground-impact-352053/ |

## Mastering

All seven files loudness-normalized to an integrated target (one-shots ≈ −15 LUFS,
loops ≈ −16 LUFS), peak-limited so post-Vorbis decode stays ≤ 0.96 FS (no clipping).
Loops verified seamless: wrap-point sample step sits inside the file's own
sample-to-step distribution, level across the wrap continuous.
