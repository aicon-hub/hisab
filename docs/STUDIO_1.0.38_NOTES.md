# Hisab Studio 2 1.0.38 (test build, Stage 1)

Signed with the release key (same as 1.0.37). Installs over 1.0.37 and keeps data.

| Review ID | Change |
|---|---|
| C4 | Split bill: the parts now always add up to the total. The first (total mod people) people pay one paisa more, you first, then the others in the order picked. Before, a remainder of 2 or more lost paise. |
| C2 | Already fixed in 1.0.36. New tests for the 29th, 30th, 31st and a 29 Feb yearly repeat across leap years. |
| S5 | Already done in 1.0.36. New test reads the manifest. |
| Q7 | Last problem screen already existed. New test: it shows the recorded crash and Share sends plain text. Nothing leaves the phone unless you share it. |
| U4 | Scroll screens with the dock leave room for the real dock height plus its margin (never under 150 dp). |
| U8 | Backup, restore and Drive errors stay on screen until you tap Dismiss. Other messages still go after about 4 seconds. |
| U3 | Quick add hint shortened to "Type it: tea 40". |
| U7 | Empty Home, Insights, Ledger, Goals and Repeats show one button (add an entry, goal or repeat). People already has Add person below. |
| U9 | No voice service: the message says to turn on Google voice typing or type instead. |
| C5, C7, C8 | Written as tests first. They passed, so these are not bugs: month end and first-day forecast, no budget means no limit, saving a converted entry is not changed by later rate edits, round-up and goals never change ledger totals. Forecast now uses whole-number maths. |

Not done in this stage: Stages 2 to 5, photo encryption, the Heat chart description, translating dynamic sentences ("Saved - each owes 33.33" stays English).
Not run on a phone. Open questions: none new.
