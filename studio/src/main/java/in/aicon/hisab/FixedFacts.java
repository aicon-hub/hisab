package in.aicon.hisab;

import java.util.Locale;

/** Questions about Hisab itself are answered from built-in text, never by the model, so nothing is invented. */
final class FixedFacts {
 static final String NOTE = "Built-in answer from Hisab. The AI model was not used.";

 private static boolean has(String q, String... words) { for (String w : words) if (q.contains(w)) return true; return false; }

 /** Null when the question is not about Hisab itself. */
 static String answer(MainActivity a, String text) {
  String q = text.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
  if (q.isEmpty()) return null;
  boolean mine = has(q, "hisab", "this app", "the app", "you ");
  if (mine && has(q, "creator", "creater", "who made", "who created", "who built", "who developed", "developer", "who owns", "owner", "author", "made by", "built by", "company", "behind")) return creator();
  if (has(q, "licence", "license", "open source", "opensource", "gpl", "is hisab free", "is it free", "source code")) return licence();
  if (has(q, "your capabilit", "what can you do", "what do you do", "what can hisab do", "what all can you", "capabilities", "your features", "what are your features", "how can you help", "what can i ask", "what can i do here")) return capabilities(a);
  if (has(q, "who are you", "what are you", "tell me about yourself", "introduce yourself", "what is hisab", "about hisab")) return about(a);
  if (has(q, "where is my data", "where are my entries", "is my data safe", "do you upload", "send my data", "my data private", "privacy")) return privacy(a);
  return null;
 }

 static String creator() {
  return "About Hisab\n\n"
   + "Muhammed Salman CC is an electronics enthusiast and an MSc Electronics student at CUSAT. He started AiCoN to share free, verified tech tools, scholarships and government schemes with students and everyday users in Kerala and India.\n\n"
   + "Creator:  Muhammed Salman CC\n"
   + "Made as part of:  AiCoN\n"
   + "AiCoN website:  https://aicon-hub.github.io\n"
   + "GitHub:  https://github.com/salman-cc\n"
   + "Portfolio:  https://salman-cc.github.io\n"
   + "Licence:  GPL-3.0, free and open source\n"
   + "Source:  https://github.com/aicon-hub/hisab\n\n"
   + "I am only the AI chat inside the app. I did not make it.";
 }

 static String licence() { return "Hisab is free and open source under the GPL-3.0 licence. It is made by Muhammed Salman CC under AiCoN. Your entries stay on your phone, or in your own Google Drive if you turn on backup. There is no Hisab server."; }

 static String about(MainActivity a) { return "I am the AI chat inside Hisab, a free money app made by Muhammed Salman CC under AiCoN. I run on a small model on your phone, so my general answers can be wrong. " + (ChatPage.read(a) ? "Read is on, so I can use your Hisab entries." : "Read is off, so I cannot see your entries."); }

 static String privacy(MainActivity a) { return "Your entries stay on this phone. If you turn on Google Drive backup, a copy goes to the hidden app-data folder of your own Google Drive, and you can encrypt it with a password. Hisab has no server. The AI model runs on this phone and nothing you type to it is sent anywhere."; }

 static String capabilities(MainActivity a) {
  boolean r = ChatPage.read(a), w = ChatPage.write(a);
  StringBuilder s = new StringBuilder("What Hisab AI can do for each part of the app, with your current switches:\n\n");
  s.append("Entries (Home, Ledger)\n");
  s.append(r ? "- Ask: recent entries, how many, spending, income, daily average, money left, top categories, compare with last month.\n" : "- Ask: needs Read. It is OFF, so I cannot see your entries.\n");
  s.append(w ? "- Change: add an entry, change the amount of the last entry, delete the last entry. Each one shows a Confirm card first.\n" : "- Change: needs Write. It is OFF, so nothing can be added, changed or deleted from chat.\n");
  s.append("Accounts and budgets\n").append(r ? "- Ask for account balances and how your budgets are doing.\n" : "- Needs Read, which is OFF.\n");
  s.append("People\n").append(r ? "- Ask who owes you and whom you owe.\n" : "- Needs Read, which is OFF.\n");
  s.append("Insights\n").append(r ? "- Ask for the numbers behind them: spending by category and period.\n" : "- Needs Read, which is OFF.\n");
  s.append("Settings, backup, categories, goals, repeats, import and export\n- Not controlled from chat yet. Open them from Settings and the screens.\n\n");
  s.append("Switches: Settings, Hisab AI. For general questions I am a small model on your phone and can be wrong.");
  return s.toString();
 }
}
