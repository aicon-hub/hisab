# Hisab Studio 2, 1.0.29 test build

- Engine: repeat penalty (1.15 over the last 64 pieces) added to the sampler. Three engine builds (base, dotprod, i8mm) rebuilt.
- App: stops and trims an answer that repeats the same chunk three times; length cap (160 / 350 / 600 pieces).
- With Read on, Hisab's exact answer is given to the model, which only words it. Numbers in the reply are checked against it; Hisab's own sentence replaces a reply with a number that is not there.
- Chat settings: speed details, Threads (Auto, 2, 3, 4, 6), Speed test. Context 3072.
- Forgot PIN: on the lock screen, Android's own screen lock confirm turns off Hisab's app lock (entries kept). Only shown if the phone has a screen lock.
- Unverified on a real phone: the new engines, the phone-lock recovery, Read/Write with a real model.
- Ledger: swipe the month (left newer, right older), arrows still work; a clear (x) button in search.
- Last 7 days on Home: swipe right for older weeks, left for newer.
