#!/usr/bin/env python3
"""Synthesize homelander flight_land.ogg — heavy low-frequency landing impact.

Layers: sub sine drop (75→30 Hz) for the chest whump, a lowpassed noise
crack transient, a short mid thud, and a low rumble tail. Output: 48 kHz
stereo OGG Vorbis at src/main/resources/assets/superheroes/sounds/homelander/.
"""
import subprocess
import sys
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/assets/superheroes/sounds/homelander/flight_land.ogg"

SR = 48000
DUR = 0.62
n = int(SR * DUR)
t = np.arange(n) / SR
rng = np.random.default_rng(1945)


def lowpass(x, cutoff, passes=2):
    # cascaded one-pole lowpass; two passes ≈ 24 dB/oct for noise shaping
    a = 1.0 - np.exp(-2.0 * np.pi * cutoff / SR)
    for _ in range(passes):
        y = np.empty_like(x)
        acc = 0.0
        for i in range(len(x)):
            acc += a * (x[i] - acc)
            y[i] = acc
        x = y
    return x


def sub_drop():
    f0, f1, dur = 75.0, 30.0, 0.36
    m = t < dur
    tt = t[m]
    freq = f1 + (f0 - f1) * np.exp(-tt / 0.10)
    phase = np.cumsum(2 * np.pi * freq / SR)
    env = np.minimum(1.0, tt / 0.004) * np.exp(-tt / 0.16)
    # hard-zero tail so the sine never stops mid-cycle
    fade = np.clip((dur - tt) / 0.05, 0.0, 1.0)
    env = env * np.sin(0.5 * np.pi * fade)
    out = np.zeros(n)
    out[m] = np.sin(phase) * env * 1.0
    return out


def crack():
    dur = 0.12
    m = t < dur
    tt = t[m]
    noise = rng.normal(0, 1, int(m.sum()))
    env = np.minimum(1.0, tt / 0.002) * np.exp(-tt / 0.035)
    out = np.zeros(n)
    out[m] = lowpass(noise, 700) * env * 0.55
    return out


def thud():
    dur = 0.07
    m = t < dur
    tt = t[m]
    env = np.minimum(1.0, tt / 0.003) * np.exp(-tt / 0.018)
    out = np.zeros(n)
    out[m] = np.sin(2 * np.pi * 130 * tt) * env * 0.45
    return out


def rumble():
    dur = 0.40
    m = (t >= 0.05) & (t < 0.05 + dur)
    tt = t[m] - 0.05
    noise = rng.normal(0, 1, int(m.sum()))
    env = np.exp(-tt / 0.15)
    out = np.zeros(n)
    out[m] = lowpass(noise, 160) * env * 0.35
    return out


mono = sub_drop() + crack() + thud() + rumble()
# soft clip then normalize to ~-1 dBFS
mono = np.tanh(mono * 1.4) / np.tanh(1.4)
mono *= 0.80 / np.max(np.abs(mono))
# light stereo decorrelation via tiny delay on one channel
d = int(0.0007 * SR)
left = mono
right = np.concatenate([np.zeros(d), mono[:-d]])
stereo = np.stack([left, right], axis=1)
pcm = (stereo * 32767).astype(np.int16)

raw = OUT.with_suffix(".raw.wav")
import wave
with wave.open(str(raw), "wb") as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm.tobytes())
subprocess.run(
    ["ffmpeg", "-y", "-v", "error", "-i", str(raw), "-c:a", "libvorbis", "-qscale:a", "5", str(OUT)],
    check=True)
raw.unlink()
print("wrote", OUT)
