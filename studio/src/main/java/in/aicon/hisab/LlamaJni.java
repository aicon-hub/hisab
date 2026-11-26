package in.aicon.hisab;

/** Native side of the optional AI engine (llama.cpp, MIT). The library is not in the APK; it is downloaded on request and loaded from private storage. */
final class LlamaJni {
 private LlamaJni() {}
 static native int load(String path, int nCtx, int threads);
 static native String chatPrompt(String[] roles, String[] texts);
 static native int begin(String prompt, float temp);
 static native byte[] next();
 static native void stop();
 static native int ctxSize();
 static native void unload();
 static native String info();
}
