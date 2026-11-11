# 0.2.2-dev verification

125 distinct passing framework methods across 18 classes. All 39 LocalUiTest methods passed in seven bounded groups. No failed or timed-out broad run is counted. Simulated Android checks are not a real-device pass.

| Class | Methods | Failures |
|---|---:|---:|
| AccountDirectoryTest | 3 | 0 |
| AccountUiTest | 9 | 0 |
| BackgroundSyncTest | 7 | 0 |
| BackupFlowTest | 3 | 0 |
| BarrierRemoteTest | 5 | 0 |
| DuePlansTest | 3 | 0 |
| LedgerControllerTest | 7 | 0 |
| LedgerDeviceSyncTest | 2 | 0 |
| LedgerSessionTest | 6 | 0 |
| LedgerSyncStoreTest | 13 | 0 |
| LedgerTransferTest | 3 | 0 |
| LocalUiTest | 39 | 0 |
| ReminderScheduleTest | 4 | 0 |
| RepaymentReviewTest | 4 | 0 |
| ResetCoordinatorTest | 3 | 0 |
| ResetStoreTest | 3 | 0 |
| SyncStoreTest | 8 | 0 |
| SyncWorkSchedulerTest | 3 | 0 |

## Methods

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
- profileDetailsDueMessagesPdfAndFontsRender
- remindersContactsAndPersonLedgerHaveExplicitLocalBoundaries
- removeUnusedEntityResetsDefaultsAndKeepsReferencedHistory
- resetConfirmationCancelAndConfirmUseCurrentLedger
- revisedThemeChoicesRenderAndEnforceGlassDarkOnly
- selectedMonthReportChartsAndFutureExclusionRender
- settledDebtClearsPrivateDuePlan
- sidewaysSwipeRevealsActionsAndDeleteStillAsks
- staleComposerReviewRejectsChangedContactAndLedger
- staleUiFormCannotWriteAndFailedSaveRollsBack
- systemThemeFollowsDeviceAndGlassStaysDark
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

Packaged APK permissions, version and unchanged signing certificate independently checked. Legacy automatic-SMS grant cleanup, no SMS permission, reviewed composer and stale contact/ledger guards pass. Real Google sign-in/Drive round trips and phone install/Play Protect outcome remain unverified. Eight earlier pure Java programs were unchanged; their previous run evidence is not relabelled as a new run.
