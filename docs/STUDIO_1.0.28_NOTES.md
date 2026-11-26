# Hisab Studio 2, 1.0.28 test build

- Ask Hisab is now only the AI model answering, with a chat screen (bubbles, pinned input, typing indicator, Copy, New chat).
- Two switches, off by default: Read my Hisab data (exact figures computed by Hisab code are given to the model, shown as "Hisab data used") and Write to my Hisab data (chat entry add, edit, delete, always behind a Confirm card).
- Chat history: asked on first use; Chat settings can change it and clear it. If no, nothing is saved.
- Engine: three llama.cpp builds (base, dotprod, i8mm) in `engine/`, chosen by the phone's CPU features (`/proc/cpuinfo`). SHA-256 checked on download. MIT licence, see engine/LLAMA_CPP_LICENSE.txt.
- The model preloads when the chat opens and is freed after 5 minutes idle.
- Hisab's own in-app keyboard is removed; the phone's keyboard is used. The PIN pad stays.
- Unverified on a real phone: the dotprod and i8mm engines, the new chat screen, Read and Write with a real model.
