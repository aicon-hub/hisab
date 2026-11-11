package in.aicon.pocket;
import java.util.*;
/** Vector-clock records. Concurrent edits are surfaced, never silently last-writer-wins.
 * Delete wins against a concurrent edit to avoid resurrecting deleted financial content.
 * Tombstones retain UUID + causal metadata, never a deleted entry's fields.
 */
public final class SyncModel {
 public static final class Record {
  public final String id,payload; public final boolean deleted; public final SortedMap<String,Long> clock;
  public Record(String id,String payload,boolean deleted,Map<String,Long> clock){
   Identifiers.uuid(id);if(!deleted&&(payload==null||payload.isEmpty()))throw new IllegalArgumentException("Missing payload");
   if(clock.size()>256)throw new IllegalArgumentException("Too many replica clocks");this.id=id;this.payload=deleted?null:payload;this.deleted=deleted;this.clock=Collections.unmodifiableSortedMap(new TreeMap<>(clock));
   for(Map.Entry<String,Long> e:this.clock.entrySet())if(!e.getKey().matches("[A-Za-z0-9_-]{1,128}")||e.getValue()<0)throw new IllegalArgumentException("Invalid clock");
  }
  public Record edit(String device,String data,boolean remove){if(deleted&&!remove)throw new IllegalStateException("Deleted records cannot be resurrected; create a new ID.");Map<String,Long> next=new TreeMap<>(clock);next.put(device,Math.addExact(next.getOrDefault(device,0L),1L));return new Record(id,data,remove,next);}
 }
 public static final class Conflict extends Exception {public final Record local,remote;public final List<Record> versions;Conflict(Record a,Record b){this(Arrays.asList(a,b));}Conflict(Collection<Record> candidates){super("Concurrent edits require review");if(candidates.size()<2||candidates.size()>256)throw new IllegalArgumentException("Invalid conflict candidates");versions=Collections.unmodifiableList(new ArrayList<>(candidates));local=versions.get(0);remote=versions.get(1);for(Record record:versions)if(!record.id.equals(local.id)||record.deleted)throw new IllegalArgumentException("Invalid edit conflict");}}
 public static int compare(Map<String,Long>a,Map<String,Long>b){boolean less=false,more=false;Set<String> keys=new HashSet<>(a.keySet());keys.addAll(b.keySet());for(String k:keys){long x=a.getOrDefault(k,0L),y=b.getOrDefault(k,0L);less|=x<y;more|=x>y;}return less&&more?2:less?-1:more?1:0;}
 public static Record merge(Record a,Record b)throws Conflict{
  if(!a.id.equals(b.id))throw new IllegalArgumentException("Different IDs");int order=compare(a.clock,b.clock);
  if(a.deleted||b.deleted){Map<String,Long> c=union(a.clock,b.clock);return new Record(a.id,null,true,c);}
  if(order==-1)return b;if(order==1)return a;
  if(Objects.equals(a.payload,b.payload))return new Record(a.id,a.payload,false,union(a.clock,b.clock));
  throw new Conflict(a,b);
 }
 /** Merge a complete operation set. Remove causal ancestors before judging conflicts;
  * otherwise two old edits could block their later, explicit conflict resolution.
  * UUID deletion is permanent, including against a malformed later resurrection.
  */
 public static Record mergeRecords(Collection<Record> input)throws Conflict {
  if(input.isEmpty())throw new IllegalArgumentException("Empty merge");
  List<Record> records=new ArrayList<>(input);String id=records.get(0).id;SortedMap<String,Long> clock=new TreeMap<>();boolean deleted=false;
  for(Record r:records){if(!id.equals(r.id))throw new IllegalArgumentException("Different IDs");clock=union(clock,r.clock);deleted|=r.deleted;}
  if(deleted)return new Record(id,null,true,clock);
  List<Record> frontier=new ArrayList<>();
  for(Record r:records){
   boolean ancestor=false;for(Record other:frontier){if(compare(r.clock,other.clock)==-1){ancestor=true;break;}}
   if(ancestor)continue;
   frontier.removeIf(other->compare(other.clock,r.clock)==-1);
   boolean duplicate=false;for(Record other:frontier)if(compare(other.clock,r.clock)==0&&Objects.equals(other.payload,r.payload)){duplicate=true;break;}
   if(!duplicate)frontier.add(r);
   if(frontier.size()>256)throw new IllegalArgumentException("Too many concurrent versions; reviewed recovery required");
  }
  Record chosen=frontier.get(0);for(Record r:frontier)if(!Objects.equals(chosen.payload,r.payload))throw new Conflict(frontier);
  return new Record(id,chosen.payload,false,clock);
 }
 public static SortedMap<String,Long> union(Map<String,Long>a,Map<String,Long>b){SortedMap<String,Long> c=new TreeMap<>(a);for(String k:b.keySet())c.put(k,Math.max(c.getOrDefault(k,0L),b.get(k)));return c;}
 public static Record resolve(Conflict c,String device,String chosenPayload,boolean delete){SortedMap<String,Long> clock=new TreeMap<>();for(Record candidate:c.versions)clock=union(clock,candidate.clock);return new Record(c.local.id,chosenPayload,delete,clock).edit(device,chosenPayload,delete);}
 public static Map<String,Record> mergeAll(Map<String,Record>a,Map<String,Record>b)throws Conflict{Map<String,Record> r=new TreeMap<>(a);for(String id:b.keySet())r.put(id,r.containsKey(id)?merge(r.get(id),b.get(id)):b.get(id));return r;}
}
