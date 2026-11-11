package in.aicon.pocket;
import java.util.*;

/** Pure cloud-reset barrier model. NOT attached to Drive transport yet.
 * Each immutable reset marker contains only a random canonical UUID. Markers must
 * survive, because deleting the barrier would let an offline replica restore data.
 * An upload carries the complete reset set observed before its edit. A later reset
 * hides that upload even when it arrived after the reset due to an in-flight request.
 */
public final class ResetBarrier {
 private ResetBarrier(){}
 public static final int MAX_RESETS=4096;
 public static SortedSet<String> canonical(Collection<String> resets){if(resets==null||resets.size()>MAX_RESETS)throw new IllegalArgumentException("Too many reset barriers; reviewed recovery required");SortedSet<String> result=new TreeSet<>();for(String id:resets){Identifiers.uuid(id);result.add(id);}return Collections.unmodifiableSortedSet(result);}
 public static SortedSet<String> combine(Collection<String> a,Collection<String> b){Set<String> both=new HashSet<>(canonical(a));both.addAll(canonical(b));return canonical(both);}
 public static final class View {
  public final SortedSet<String> known;public final String fingerprint;
  public View(Collection<String> known){this.known=canonical(known);try{java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");for(String id:this.known){digest.update(id.getBytes(java.nio.charset.StandardCharsets.US_ASCII));digest.update((byte)'\n');}StringBuilder hex=new StringBuilder();for(byte n:digest.digest())hex.append(String.format(Locale.ROOT,"%02x",n&255));fingerprint=hex.toString();}catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}}
  public View reset(String id){Identifiers.uuid(id);if(known.contains(id))throw new IllegalArgumentException("A new reset requires a new marker ID");Set<String> all=new HashSet<>(known);all.add(id);return new View(all);}
  /** A missing locally observed barrier is not evidence it disappeared safely. */
  public Check inspect(Collection<String> remote){SortedSet<String> actual=canonical(remote);if(!actual.containsAll(known))throw new IllegalStateException("Cloud reset history is incomplete; no sync or upload");return new Check(this,new View(actual));}
 }
 public static final class Check {
  public final View before,remote;public final boolean resetObserved;
  private Check(View before,View remote){this.before=before;this.remote=remote;resetObserved=!before.known.equals(remote.known);}
  public void requireUnchanged(){if(resetObserved)throw new IllegalStateException("Cloud data was reset; quarantine local records before continuing");}
  /** Financial operations written without every current barrier can never reappear. */
  public boolean accepts(Collection<String> operationResetIds){SortedSet<String> operation=canonical(operationResetIds);if(!remote.known.containsAll(operation))throw new IllegalStateException("Operation refers to an unseen reset; re-read barriers before sync");return remote.known.equals(operation);}
 }
 /** Explicit acknowledgment must atomically clear/quarantine old visible records and
  * unsent outbox contents before recording the new barrier set. A caller may retain
  * a private local-only recovery copy, never upload it as an incidental reconnect.
  */
 public interface ApplyReset {void discardOldAndCommit(View replacement)throws Exception;}
 public static View acknowledge(Check check,ApplyReset atomic)throws Exception{if(!check.resetObserved)return check.before;atomic.discardOldAndCommit(check.remote);return check.remote;}
}
