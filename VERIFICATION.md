# Final candidate verification

130 distinct successful framework test methods across 19 classes, in bounded groups. Framework simulation only. All 38 UI methods passed in five groups after whole-class native UI runs exhausted host memory; those failed runs are not counted.

| Class | Methods | Failures |
|---|---:|---:|
| AccountDirectoryTest | 3 | 0 |
| AccountUiTest | 9 | 0 |
| AutoSmsTest | 5 | 0 |
| BackgroundSyncTest | 7 | 0 |
| BackupFlowTest | 3 | 0 |
| BarrierRemoteTest | 5 | 0 |
| DuePlansTest | 4 | 0 |
| LedgerControllerTest | 7 | 0 |
| LedgerDeviceSyncTest | 2 | 0 |
| LedgerSessionTest | 6 | 0 |
| LedgerSyncStoreTest | 13 | 0 |
| LedgerTransferTest | 3 | 0 |
| LocalUiTest | 38 | 0 |
| ReminderScheduleTest | 4 | 0 |
| RepaymentReviewTest | 4 | 0 |
| ResetCoordinatorTest | 3 | 0 |
| ResetStoreTest | 3 | 0 |
| SyncStoreTest | 8 | 0 |
| SyncWorkSchedulerTest | 3 | 0 |

Eight pure programs passed at the earlier candidate checkpoint and those pure classes have not changed since: Ledger 34,570; hints 14 examples; sync model 50,008; vector properties 70,000; account scope 80,007; ledger integrity 20,008; engine 3,008; reset barriers 130,003. These are not real-world accuracy, transport or device benchmarks.

Final private APK: in.aicon.pocket.test, versionCode 3 / 0.2.1-dev, min 26 / target 35 / compile 36. v2 signature verifies; preview certificate SHA1 D0:A9:1B:7D:52:13:8B:44:90:1F:AB:2E:FB:91:88:53:A8:20:5A:D8.

APK SHA256: `71e886e11f7e90814de25e9d80715db2c23c40c0214d01c66e8f7948fa7ce2cb`
Bytes: 9554643

Permissions inspected: INTERNET, READ_CONTACTS, POST_NOTIFICATIONS, SEND_SMS, RECEIVE_BOOT_COMPLETED and expected background permissions. No READ_SMS/RECEIVE_SMS, biometric or fingerprint permission. SEND_SMS is requested only for explicit experimental automatic-SMS opt-in; it is not proof the installer or device will grant it.

No real Google consent/sign-in/Drive round trip, contact/photo picker, carrier SMS, WhatsApp send, notification delivery/reboot, launcher upgrade or phone performance pass is claimed. No live messages were sent during testing. Automatic-SMS tests use only simulated telephony.
