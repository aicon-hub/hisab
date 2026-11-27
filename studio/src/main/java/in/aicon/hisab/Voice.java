package in.aicon.hisab;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;

/** Voice typing for the quick-add bar. Audio goes to the phone's own speech service; Hisab keeps nothing. */
final class Voice {
 private Voice() {}
 interface Heard { void onText(String s); }

 static void listen(final MainActivity a, final Heard cb) { listen(a, cb, null); }

 static void listen(final MainActivity a, final Heard cb, final java.util.function.Consumer<Boolean> active) {
  if (!SpeechRecognizer.isRecognitionAvailable(a)) { a.toast("Voice typing is not available on this phone", null); return; }
  try {
   final SpeechRecognizer sr = SpeechRecognizer.createSpeechRecognizer(a);
   Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, L.isMl() ? "ml-IN" : "en-IN")
    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
   sr.setRecognitionListener(new RecognitionListener() {
    @Override public void onResults(Bundle r) { sr.destroy(); if (active != null) active.accept(false); ArrayList<String> m = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (m != null && !m.isEmpty()) cb.onText(m.get(0)); else a.toast("Could not hear that", null); }
    @Override public void onError(int e) { sr.destroy(); if (active != null) active.accept(false); a.toast(e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ? "Could not hear that" : "Voice typing stopped", null); }
    @Override public void onReadyForSpeech(Bundle p) { if (active != null) active.accept(true); a.toast("Listening...", null); }
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float v) {}
    @Override public void onBufferReceived(byte[] b) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onPartialResults(Bundle p) {}
    @Override public void onEvent(int t, Bundle p) {}
   });
   sr.startListening(i);
  } catch (Exception e) { a.toast("Voice typing is not available on this phone", null); }
 }
}
