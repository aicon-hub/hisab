package in.aicon.hisab;

import java.util.*;

final class CategoryIcons {
 private CategoryIcons() {}
 static final String[][] RULES={
  {"food","food tea coffee lunch dinner breakfast snack snacks biryani juice meal chai ചായ കാപ്പി ഭക്ഷണം ഹോട്ടൽ"},
  {"groceries","milk vegetables fruits groceries grocery rice eggs പാൽ അരി പലചരക്ക്"},
  {"transport","auto bus metro cab taxi uber ola train ticket parking ഓട്ടോ ബസ് ട്രെയിൻ"},
  {"fuel","petrol diesel fuel പെട്രോൾ ഡീസൽ ഇന്ധനം"},
  {"rent","rent hostel വാടക"},{"bills","bill bills electricity gas eb വൈദ്യുതി ബിൽ"},
  {"wifi","wifi internet broadband fiber"},{"phone","recharge mobile phone ഫോൺ റീചാർജ്"},
  {"health","medicine pharmacy doctor hospital health മരുന്ന് ആശുപത്രി ആരോഗ്യം"},
  {"fitness","gym fitness yoga sport sports exercise ജിം വ്യായാമം"},{"pet","pet pets dog cat vet"},
  {"fun","movie cinema game fun show സിനിമ വിനോദം"},{"subscription","netflix prime spotify youtube subscription ott"},
  {"education","book books fees course exam school college education പഠനം പുസ്തകം ഫീസ്"},
  {"shopping","shirt shoes clothes dress shopping വസ്ത്രം ഷോപ്പിംഗ്"},
  {"travel","flight hotel holiday tourism travel trip യാത്ര"},{"gift","gift gifts birthday സമ്മാനം"},
  {"charity","charity donation donations zakat sadaqah സകാത്ത് സദഖ ദാനം"},
  {"repair","repair maintenance service mechanic അറ്റകുറ്റപ്പണി"},{"beauty","salon haircut beauty cosmetics barber മുടി"},
  {"work","work office freelance client salary income ജോലി ശമ്പളം"},{"baby","baby child kids diaper toy"},
  {"insurance","insurance takaful"},{"tax","tax gst income-tax"},{"water","water വെള്ളം"},{"laundry","laundry wash cleaning"},
  {"electronics","electronics laptop computer keyboard mouse gadget"},
  {"printer","print printing photocopy xerox scan stationery സ്റ്റേഷനറി പ്രിന്റ്"},
  {"pen","pen pencil stationery ഓട്ടോ"} ,
  {"cap","tuition exam exams graduation degree scholarship ട്യൂഷൻ പരീക്ഷ"},
  {"cycle","cycle bicycle bike"}
 };
 static String suggest(String text){if(text==null)return "other";String s=text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{M}0-9-]+"," ").trim();Set<String> words=new HashSet<>(Arrays.asList(s.split("\\s+")));for(String[] r:RULES)for(String w:r[1].split(" "))if(words.contains(w))return r[0];return "other";}
 static String existingCategory(String icon){switch(icon){case "rent":case "wifi":case "subscription":case "water":case "insurance":case "tax":return "Bills";case "fitness":return "Health";case "electronics":case "beauty":case "baby":case "pet":case "repair":case "laundry":return "Shopping";default:for(String[] r:HomePage.WORDS)if(r[0].equalsIgnoreCase(icon))return r[0];return null;}}
}
