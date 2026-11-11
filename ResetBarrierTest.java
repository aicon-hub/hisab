import in.aicon.pocket.*;import java.util.*;
public class ResetBarrierTest {
 static int cases;static void check(boolean b){cases++;if(!b)throw new AssertionError(cases);}
 public static void main(String[] args)throws Exception{
  Random random=new Random(5643);ResetBarrier.View empty=new ResetBarrier.View(Collections.emptySet());
  for(int n=0;n<10000;n++){
   String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString();ResetBarrier.View first=empty.reset(a),second=empty.reset(b);Set<String> union=ResetBarrier.combine(first.known,second.known);check(ResetBarrier.combine(first.known,second.known).equals(ResetBarrier.combine(second.known,first.known)));ResetBarrier.Check changed=empty.inspect(union);check(changed.resetObserved);check(!changed.accepts(empty.known));check(!changed.accepts(first.known));check(!changed.accepts(second.known));check(changed.accepts(union));try{changed.requireUnchanged();throw new AssertionError();}catch(IllegalStateException expected){cases++;}final boolean[] wiped={false};ResetBarrier.View adopted=ResetBarrier.acknowledge(changed,next->{wiped[0]=true;check(next.known.equals(union));});check(wiped[0]);check(adopted.known.equals(union));check(!adopted.inspect(union).resetObserved);try{adopted.inspect(first.known);throw new AssertionError();}catch(IllegalStateException expected){cases++;}try{empty.inspect(empty.known).accepts(first.known);throw new AssertionError();}catch(IllegalStateException expected){cases++;}
  }
  ResetBarrier.View next=empty.reset(UUID.randomUUID().toString());try{ResetBarrier.acknowledge(empty.inspect(next.known),replacement->{throw new java.io.IOException("rollback");});throw new AssertionError();}catch(java.io.IOException expected){check(empty.known.isEmpty());}try{empty.reset("1-1-1-1-1");throw new AssertionError();}catch(IllegalArgumentException expected){cases++;}check(new ResetBarrier.View(Arrays.asList(next.known.iterator().next())).fingerprint.equals(next.fingerprint));System.out.println(cases+" reset barrier/no-offline-resurrection model assertions passed. Transport remains unwired.");
 }
}
