# Hisab Studio 2 1.0.26 (test build)

- Hisab AI models screen (Ask Hisab > AI models, and Settings > Hisab AI models). Optional. Nothing downloads unless you tap Download and confirm.
- Models grouped by memory tier (4, 6, 8, 12, 16 GB), each with size, minimum memory and a note. Q4_K_M files from Hugging Face; sizes and SHA-256 read from the Hugging Face file listings on 27 Nov 2026.
- "Allow and check" reads memory, CPU cores and ABI, SoC name, GPU name (a throwaway GL context), free storage. NPU cannot be read by an app (shows what Android exposes). Marks: Suitable / Borderline / Not suitable by a rule of thumb, not a speed test. Nothing is sent anywhere. No new permissions.
- Download: animated progress bar to 100%, speed, pause and resume (HTTP range), SHA-256 check (a bad file is discarded), free-storage check, delete.
- A downloaded model is stored only. Ask Hisab still uses its own rules. A runtime that runs the model is a separate prototype.
- Bottom space under tab pages raised so the floating dock no longer covers the last rows.
- App size about 4.6 MB. Not tested on a real phone. GPL-3.0.
