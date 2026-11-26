# Hisab AI engine (optional download)

`libhisabllm.so` is a small JNI bridge (`hisabllm.cpp`) over llama.cpp (MIT licence, ggml authors, see LLAMA_CPP_LICENSE.txt).
It is NOT in the APK. The app downloads it from the release asset `libhisabllm-arm64.so`, checks its SHA-256 and loads it from private storage.

Built with: llama.cpp tag v1.1.0 (commit 171527522610996f5f0acf12334e185156ca8f44), Android NDK r27c (27.2.12479018), arm64-v8a, android-26, Release, `-O3`,
static llama and ggml linked into one shared library, no OpenMP, no backend plug-ins, generic armv8 CPU code (no dotprod or i8mm paths yet).

```
git clone --branch v1.1.0 https://github.com/ggml-org/llama.cpp /tmp/llama.cpp
cmake -S engine -B build -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_FLAGS="-O3 -ffunction-sections -fdata-sections" -DCMAKE_CXX_FLAGS="-O3 -ffunction-sections -fdata-sections"
cmake --build build --target hisabllm
llvm-strip --strip-unneeded build/libhisabllm.so -o libhisabllm.so
```

## Variants (1.0.28)
Three builds from the same source: base (armv8-a), dotprod (`-march=armv8.2-a+dotprod`) and i8mm (`-march=armv8.6-a+i8mm+dotprod`, check CMake flags used in the build directories). The app reads the CPU features and downloads the matching file. The SHA-256 of each is in AiEngine.java.
