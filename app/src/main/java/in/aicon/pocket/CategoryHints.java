package in.aicon.pocket;
import java.util.*;
public final class CategoryHints {
 public static String normalize(String s){return java.text.Normalizer.normalize(s,java.text.Normalizer.Form.NFC).toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");}
 public static boolean isEnglishInput(String s){for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isLetter(c)&&!(c>='A'&&c<='Z')&&!(c>='a'&&c<='z'))return false;}return true;}
 public static String hint(String input){String s=normalize(input);if(s.isEmpty()||!isEnglishInput(s))return null;Set<String> words=new HashSet<String>(Arrays.asList(s.split("[^\\p{L}\\p{M}0-9]+")));String[][] rules={{"Food","swiggy","zomato","groceries","grocery","cafe","restaurant","lunch","dinner","breakfast"},{"Travel","uber","ola","bus","metro","train","petrol","diesel"},{"Home","rent"},{"Study","tuition","textbook","textbooks","college"},{"Health","pharmacy","hospital","medicine","medicines"},{"Bills","electricity","recharge","broadband"}};String found=null;for(String[] r:rules)for(int i=1;i<r.length;i++)if(words.contains(r[i])){if(found!=null&&!found.equals(r[0]))return null;found=r[0];}return found;}
}
