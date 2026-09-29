# Contract changes — visual-core-homelander

Format: `## <date> <id>` / Problem / Smallest change / Status (`proposed|accepted|rejected`) / Link.

## 2026-09-29 homelander-flight-laser-sound-durations

Problem: the seven flight/laser contract rows recorded the synthesized placeholder
durations (takeoff 600 ms, boost 1200 ms, land 500 ms, laser charge 300 ms,
laser release 400 ms, both loops 1000 ms). The real sourced replacements land
outside the ±30 % tolerance, so `oneShotSoundDurationsWithinTolerance` fails.

Smallest change: `homelander_pilot.json` `durationMs` updated to the measured
values of the shipped files — takeoff 1429 ms, `flight_loop` 6007 ms,
boost 2085 ms, land 1482 ms, laser charge 930 ms, `laser_loop` 5201 ms,
laser release 1151 ms. No event names, file paths or loop flags changed;
`sounds.json` untouched. All durations sit inside the task spec ranges
(takeoff 0.6–1.5 s, loop 4–8 s, boost 1–2.5 s, land 0.5–1.5 s,
charge 0.3–1 s, laser loop 3–6 s, release 0.4–1.5 s) with takeoff/land at
~1.5 s only marginally over their spec ceilings — richer real-source tails
were preferred over hard truncation.

Status: proposed

Link: branch `devin/1790691597-homelander-sounds`; provenance in
`art-source/sounds/homelander/SOURCES.md`.
