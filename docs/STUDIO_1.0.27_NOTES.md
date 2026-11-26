# Hisab Studio 2 1.0.27 (test build)

- Ask Hisab has a switch at the very top: "Hisab only answers" (own data, rules, sources, no model) and "All-model answers" (a downloaded model answers like a normal AI chat from general knowledge; it does not see your Hisab data).
- Models run on this phone through an optional AI engine (about 7 MB, llama.cpp, MIT licence) downloaded from the release asset, SHA-256 checked, stored in private storage. The APK stays about 4.6 MB. Engine source and build notes are in /engine.
- Pick among your downloaded models; each shows what it can do. Streaming answer, Stop button, and timing on screen (pieces per second, time to first word, model load time).
- The bridge was run for real on a PC (x86) with Qwen2.5 0.5B and Gemma 2 2B: load, chat template, prompt, streaming, stop and unload all work. It has NOT been run on an Android phone yet; the arm64 build is unverified on a device.
- Files and images: not in this build (next build: text extracted in the app, then given to the model). Gemma 2 2B is text-only.
- Not tested on a real phone for this feature. GPL-3.0.
