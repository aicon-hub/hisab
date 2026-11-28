# Hisab Studio 2 1.0.36 (test build)

- PIN hash is now PBKDF2-HMAC-SHA256, 120000 rounds, with a try limit (5 tries, then 30 s pauses doubling to 15 min). Old PINs upgrade on the next correct entry.
- Monthly and yearly repeats keep their original day (31st stays the 31st).
- TalkBack: tappable tiles speak their text as buttons; charts have descriptions.
- Malayalam: about 240 more strings.
- Reminder receivers not exported.
- Not done: database encryption (SQLCipher would add several MB per ABI), release signing key (unchanged), Heat chart description, long AI texts in Malayalam.
- Not tested on a phone or with a real screen reader.
