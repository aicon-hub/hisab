# 0.2.3-dev verification

128 distinct passing framework methods across 18 classes, including all 42 LocalUiTest methods. Bounded class/UI groups passed. A final focused rerun passed debt save/repayment/cascade delete and stale/explicit messaging protections. This is Android-framework simulation, not phone testing or a performance comparison with another app.

The changed UI tests now require Glass to follow light/dark/On device instead of enforcing the removed dark-only restriction. Defaults changed for a new/reset ledger only; saved settings and imported backup choices remain valid.

Visual review: native light/dark Home, larger centred icons, Profile, entry/large-font entry, weekly/month/year reports, person/contact menu and existing export/privacy views. The revised near-black Forest dark Home was reviewed after rejecting the first grey rendering. Live phone keyboard, scroll, performance and colour rendering still need verification.

| Class | Passing methods |
|---|---:|
| AccountDirectoryTest | 3 |
| AccountUiTest | 9 |
| BackgroundSyncTest | 7 |
| BackupFlowTest | 3 |
| BarrierRemoteTest | 5 |
| DuePlansTest | 3 |
| LedgerControllerTest | 7 |
| LedgerDeviceSyncTest | 2 |
| LedgerSessionTest | 6 |
| LedgerSyncStoreTest | 13 |
| LedgerTransferTest | 3 |
| LocalUiTest | 42 |
| ReminderScheduleTest | 4 |
| RepaymentReviewTest | 4 |
| ResetCoordinatorTest | 3 |
| ResetStoreTest | 3 |
| SyncStoreTest | 8 |
| SyncWorkSchedulerTest | 3 |

## Exact passing methods

### in.aicon.pocket.AccountDirectoryTest
- forgetReturnsToLocalWithoutClaimingCloudDeletion
- unknownAndCorruptSelectionCannotOpenLedger
- verificationDoesNotSelectOrAdoptAndSelectionSurvivesReopen

### in.aicon.pocket.AccountUiTest
- conflictDescriptionShowsMoneyAsCurrencyNotMinorUnits
- disconnectConfirmationCancelKeepsAccountAndPermissionRequestUnstarted
- documentPickerReturnFromClosedLedgerIsDiscarded
- localAndRememberedAccountScreensAndSeparateLedgerSwitch
- oldResetConfirmationCannotResetDifferentSelectedLedger
- postedBackupActionAndReplacementTicketCannotCrossLedgerSwitchOrEdit
- repaymentReviewRendersOriginalDebtAndBothUnchosenPayments
- replacementDialogCannotRestoreAfterNewLocalChanges
- resetDialogCancelCannotClearMoneyAndNamesExpectedAccount

### in.aicon.pocket.BackgroundSyncTest
- differentGoogleIdentityNeverRunsAndRequiresReconnect
- pendingDisconnectStopsEvenAlreadyAuthorizedBackgroundWork
- pendingResetStopsBeforeAuthorizationAndEdit
- queuedOldAccountCannotAuthorizeWhenSelectionChanged
- rateLimitsRetryButPermissionRequestsStayQuiet
- selectedAccountSyncClearsItsTransientTokenAndSavesOnlyStatus
- selectionChangeDuringAuthorizationStopsBeforeAnySync

### in.aicon.pocket.BackupFlowTest
- decryptReachesReplacementConfirmationWithoutMutatingLedgerVersion
- decryptedReplacementCannotOverwriteInterveningNewEntry
- encryptedBackupPassphraseFormDoesNotWriteOrInvalidateLedger

### in.aicon.pocket.BarrierRemoteTest
- appendInFlightAtResetNeverReturnsAfterBarrierAcknowledgment
- futureBarrierOperationForcesRecheckInsteadOfSilentDrop
- missingResetHistoryAndUnknownDeletionStopBeforeEffects
- retiredLateUploadIsScrubbedOnNextSuccessfulSyncCycle
- wireRejectsHiddenPayloadInMarkerAndDuplicateBarrierIds

### in.aicon.pocket.DuePlansTest
- changedDateClearsAutomaticGrant
- dateTriggersOnlyAfterPromisedDayAndNoRepeat
- legacyAutomaticSmsGrantIsScrubbedWithoutSending

### in.aicon.pocket.LedgerControllerTest
- disconnectRevokesExactAccountThenReturnsLocalWithoutErasingCachedMoney
- failedDisconnectPersistsAndBlocksReauthorizationUntilExplicitRetry
- forgetDoesNotDeleteCloudOrMoveCachedLedger
- restoringAccountCacheRequiresReconnectAndNeverReusesAnotherToken
- staleDisconnectTargetCannotRevokeAnotherSelectedAccount
- staleNetworkCompletionCannotRepaintClosedController
- verifiedIdentityNeverSelectsCopiesOrSyncs

### in.aicon.pocket.LedgerDeviceSyncTest
- localCurrencyRelabelAttemptRollsBackPreferencesAndOutbox
- twoDevicesKeepOfflineRepaymentLinkedAcrossAccountAndPersonRenames

### in.aicon.pocket.LedgerSessionTest
- formCannotOverwriteBackgroundMergeThroughAnotherDatabaseHandle
- invalidatedFormsAndOtherSessionTicketsCannotWrite
- localSessionMigratesOriginalSchemaWithoutUploading
- pendingResetBlocksEditsAndOldFormsBeforeAnyUpload
- sessionEditCapturesPreferencesCatalogAndMoneyTogether
- validationFailureRollsBackMoneyCatalogPreferenceAndOutbox

### in.aicon.pocket.LedgerSyncStoreTest
- captureRollbackRestoresOriginalLedger
- catalogRenameResolvesExistingReferenceAndPreferenceRoundTrips
- deleteGoalAndRecreateGetsNewIdentity
- emptyCatalogPlaceholdersNeverReuseDeletedCloudIdentities
- firstEmptyCloudSyncKeepsUncommittedStarterCatalogAndEmptyHistory
- invalidCombinedRepaymentsKeepAllTablesAndOutbox
- mergedQuickDefaultCannotSilentlySelectMissingOrWrongAccount
- multipleConflictChoicesApplyWholeCandidateLedgerOnlyAfterFinalReview
- sameSeedLabelsHaveSameIdsAcrossFreshDevices
- staleReviewDoesNotOverwriteNewLocalEdit
- threeWayConflictRetainsEveryChoiceAndResolutionDominatesAll
- transactionsBudgetsGoalsCaptureAndReloadAtomically
- wrongEntityReferenceAndMixedCurrencyRejectAtomically

### in.aicon.pocket.LedgerTransferTest
- explicitCopyKeepsSourceAndLinksAndCapturesNewAccountClock
- failedCopyTransactionKeepsEmptyTargetAndSource
- repeatCopyAndNonlocalSourceAreRejectedWithoutReplacing

### in.aicon.pocket.LocalUiTest
- budgetAndGoalEditingPreserveIdentity
- contactMenuOnlyOffersReviewedMessages
- customizationBackupRoundtripIncludesNewChoices
- customizationEditorRendersAndSaves
- customizationRejectsDuplicatesAndFailedRestoreIsAtomic
- customizationRenamePreservesDebtLinksTransfersBudgetsAndBackup
- darkMalayalamRenders
- debtEntryPartialRepaymentOverpaymentAndCascadeDelete
- debtPeopleRender
- deleteAllLocalClearsPersonalCatalogAndResetsCustomDefaults
- deviceFeedbackBackHomeColoursAndBranding
- disappearedDeleteTargetNeverDeletesOtherRecords
- floatingDockAnimationHasIntermediateFrames
- glassHomeBottomAndLargeFontsRemainReachable
- invalidPreferenceAndFractionalBackupLeaveDataUnchanged
- largeIconChooserRenders
- largerFontEntryRemainsScrollable
- mixedPersonLedgerAndPhotoAreDevicePrivate
- monthNavigationDayGroupsAndSwipeNeverDeletes
- monthlyChartsPhotosAndFloatingGlassRender
- oldBackupWithMultipleBudgetsAndGoalsMigrates
- openEntryCannotRecreateRenamedAccountFromStaleChoices
- originalCategoryMarksAndLauncherVariants
- overviewAndEntryRender
- pdfPaginationAndMalayalamStayWithinPage
- polishedSettingsLightAndDarkMalayalamRender
- preferencesFormattingDefaultsRenameAndBackup
- premiumHomeGraphExcludesLoansAndKeyboardFormSaves
- premiumPalettesHaveReadablePrimaryTextAndLargerIcons
- profileDetailsDueMessagesPdfAndFontsRender
- remindersContactsAndPersonLedgerHaveExplicitLocalBoundaries
- removeUnusedEntityResetsDefaultsAndKeepsReferencedHistory
- resetConfirmationCancelAndConfirmUseCurrentLedger
- revisedThemeChoicesRenderInBothModes
- revisedTrueDarkHomeAndAllReportPeriodsRender
- selectedMonthReportChartsAndFutureExclusionRender
- settledDebtClearsPrivateDuePlan
- sidewaysSwipeRevealsActionsAndDeleteStillAsks
- staleComposerReviewRejectsChangedContactAndLedger
- staleUiFormCannotWriteAndFailedSaveRollsBack
- systemThemeFollowsDeviceForBothSurfaces
- twoThousandEntryLocalMutationPassesDirectAndBackupValidation

### in.aicon.pocket.ReminderScheduleTest
- defaultsOffAndTwoLocalTimes
- deniedPermissionDoesNotScheduleAndBootReschedulesOnlyEnabled
- notificationHasNoPrivateAmountsOrPeople
- timezoneAndNextDayRollover

### in.aicon.pocket.RepaymentReviewTest
- invalidCombinedRepaymentsStageWithoutChangingMoneyThenExplicitDeletionCommits
- severalOverpaidDebtsKeepEveryChoiceStagedUntilWholeLedgerIsValid
- staleOrNonPaymentChoicesCannotRemovePrincipal
- syncEngineRetainsInvalidMergedCandidateBeforeAnyUploadOrMoneyReplacement

### in.aicon.pocket.ResetCoordinatorTest
- failedCleanupRetainsIntentAndDoesNotRepublishOrResurrect
- lostPublishResponseRetriesSameMarkerAndReservation
- noIntentOrAccountGuardPreventsAnyRemoteMutation

### in.aicon.pocket.ResetStoreTest
- explicitAcknowledgmentClearsAllFinancialAndOutboxBytesAtomically
- rollbackKeepsFinancialDataPendingUploadsAndPriorBarrierView
- untransactionalOrStaleAcknowledgmentCannotClearLedger

### in.aicon.pocket.SyncStoreTest
- accountMismatchAndUntransactionalEditAreRejected
- concurrentLocalEditRejectsOldMerge
- localEditAndDeleteAreAtomicAndContentFree
- remoteMergeRetainsTombstonesAndRejectsRewind
- reservationSurvivesReopenAndAcknowledgesOnlyExactOperation
- rollbackDoesNotLeaveOutboxOrRecord
- shortenedIdsAndInvalidCatalogNumbersAreRejected
- strictJsonRejectsContentInDeletionAndFractionalClock

### in.aicon.pocket.SyncWorkSchedulerTest
- reopeningSameLedgerDoesNotCancelItsPendingEditJob
- repeatedEditEnqueueKeepsOnePendingJobAndLocalSelectionCancelsIt
- requestsUseSelectedSubjectNetworkConstraintsAndInexactThirtyMinuteCadence

## Still unverified

Real-phone install/Play Protect, native Google sign-in and own-Drive upload/download, account switch/revoke/reset recovery, Contacts/photo picker behavior, upgrade/data retention, WhatsApp/SMS handoff, keyboard/insets, notifications/reboot/background delivery and performance. Do not use important data without a separate backup. Google External Testing and non-sensitive appdata scopes are unchanged. Public brand verification and Production publication have not been submitted.
