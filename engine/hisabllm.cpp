// Hisab on-device engine: a thin JNI bridge over llama.cpp (MIT). Loaded at run time from a file the user chose to download.
#include <jni.h>
#ifdef __ANDROID__
#include <android/log.h>
#endif
#include <string>
#include <vector>
#include <atomic>
#include "llama.h"

static llama_model * g_model = nullptr;
static llama_context * g_ctx = nullptr;
static llama_sampler * g_smpl = nullptr;
static const llama_vocab * g_vocab = nullptr;
static std::atomic<bool> g_stop{false};
static int g_ctx_size = 0, g_pos = 0;
static bool g_init = false;

extern "C" JNIEXPORT jint JNICALL Java_in_aicon_hisab_LlamaJni_load(JNIEnv * env, jclass, jstring jpath, jint nctx, jint threads) {
    if (!g_init) { llama_backend_init(); g_init = true; }
    const char * p = env->GetStringUTFChars(jpath, nullptr);
    llama_model_params mp = llama_model_default_params();
    g_model = llama_model_load_from_file(p, mp);
    env->ReleaseStringUTFChars(jpath, p);
    if (!g_model) return 1;
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = nctx; cp.n_batch = 512; cp.n_ubatch = 512; cp.n_threads = threads; cp.n_threads_batch = threads;
    g_ctx = llama_init_from_model(g_model, cp);
    if (!g_ctx) { llama_model_free(g_model); g_model = nullptr; return 2; }
    g_vocab = llama_model_get_vocab(g_model); g_ctx_size = (int) llama_n_ctx(g_ctx);
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL Java_in_aicon_hisab_LlamaJni_chatPrompt(JNIEnv * env, jclass, jobjectArray roles, jobjectArray texts) {
    if (!g_model) return nullptr;
    int n = env->GetArrayLength(roles);
    std::vector<std::string> r(n), t(n); std::vector<llama_chat_message> m(n);
    for (int i = 0; i < n; i++) {
        jstring a = (jstring) env->GetObjectArrayElement(roles, i); jstring b = (jstring) env->GetObjectArrayElement(texts, i);
        const char * ca = env->GetStringUTFChars(a, nullptr); const char * cb = env->GetStringUTFChars(b, nullptr);
        r[i] = ca; t[i] = cb; env->ReleaseStringUTFChars(a, ca); env->ReleaseStringUTFChars(b, cb);
        m[i] = { r[i].c_str(), t[i].c_str() };
    }
    const char * tmpl = llama_model_chat_template(g_model, nullptr);
    size_t total = 256; for (auto & s : t) total += s.size() * 2 + 64;
    std::vector<char> buf(total);
    int len = llama_chat_apply_template(tmpl, m.data(), n, true, buf.data(), (int) buf.size());
    if (len > (int) buf.size()) { buf.resize(len); len = llama_chat_apply_template(tmpl, m.data(), n, true, buf.data(), (int) buf.size()); }
    if (len < 0) return nullptr;
    // Java gets the text through a UTF-8 byte path elsewhere; the template output is built from what Java sent, so it is valid
    return env->NewStringUTF(std::string(buf.data(), len).c_str());
}

// Returns the number of prompt tokens, or a negative error. temp <= 0 means greedy.
extern "C" JNIEXPORT jint JNICALL Java_in_aicon_hisab_LlamaJni_begin(JNIEnv * env, jclass, jstring jprompt, jfloat temp) {
    if (!g_ctx) return -1;
    g_stop = false;
    const char * p = env->GetStringUTFChars(jprompt, nullptr); std::string prompt(p); env->ReleaseStringUTFChars(jprompt, p);
    bool addSpecial = prompt.rfind("<bos>", 0) != 0;
    int nt = -llama_tokenize(g_vocab, prompt.c_str(), (int) prompt.size(), nullptr, 0, addSpecial, true);
    if (nt <= 0) return -2;
    if (nt >= g_ctx_size - 8) return -3;
    std::vector<llama_token> tok(nt);
    if (llama_tokenize(g_vocab, prompt.c_str(), (int) prompt.size(), tok.data(), nt, addSpecial, true) < 0) return -2;
    llama_memory_clear(llama_get_memory(g_ctx), true); g_pos = 0;
    if (g_smpl) { llama_sampler_free(g_smpl); g_smpl = nullptr; }
    g_smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (temp <= 0.0f) llama_sampler_chain_add(g_smpl, llama_sampler_init_greedy());
    else { llama_sampler_chain_add(g_smpl, llama_sampler_init_top_k(40)); llama_sampler_chain_add(g_smpl, llama_sampler_init_top_p(0.9f, 1)); llama_sampler_chain_add(g_smpl, llama_sampler_init_temp(temp)); llama_sampler_chain_add(g_smpl, llama_sampler_init_dist(1234)); }
    for (int i = 0; i < nt; i += 512) {
        if (g_stop) return -4;
        int n = std::min(512, nt - i);
        llama_batch b = llama_batch_get_one(tok.data() + i, n);
        if (llama_decode(g_ctx, b) != 0) return -5;
    }
    g_pos = nt;
    return nt;
}

static std::vector<llama_token> g_last;
// Next piece as UTF-8 bytes (may be empty). Returns null at end of text, stop, or full context.
extern "C" JNIEXPORT jbyteArray JNICALL Java_in_aicon_hisab_LlamaJni_next(JNIEnv * env, jclass) {
    if (!g_ctx || !g_smpl || g_stop) return nullptr;
    if (g_pos >= g_ctx_size - 1) return nullptr;
    llama_token t = llama_sampler_sample(g_smpl, g_ctx, -1);
    if (llama_vocab_is_eog(g_vocab, t)) return nullptr;
    char buf[256]; int n = llama_token_to_piece(g_vocab, t, buf, sizeof(buf), 0, false);
    if (n < 0) n = 0;
    llama_batch b = llama_batch_get_one(&t, 1);
    if (llama_decode(g_ctx, b) != 0) return nullptr;
    g_pos++;
    jbyteArray out = env->NewByteArray(n); if (n > 0) env->SetByteArrayRegion(out, 0, n, (const jbyte *) buf);
    return out;
}

extern "C" JNIEXPORT void JNICALL Java_in_aicon_hisab_LlamaJni_stop(JNIEnv *, jclass) { g_stop = true; }
extern "C" JNIEXPORT jint JNICALL Java_in_aicon_hisab_LlamaJni_ctxSize(JNIEnv *, jclass) { return g_ctx_size; }
extern "C" JNIEXPORT void JNICALL Java_in_aicon_hisab_LlamaJni_unload(JNIEnv *, jclass) {
    if (g_smpl) { llama_sampler_free(g_smpl); g_smpl = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}
extern "C" JNIEXPORT jstring JNICALL Java_in_aicon_hisab_LlamaJni_info(JNIEnv * env, jclass) { return env->NewStringUTF(llama_print_system_info()); }
