package in.aicon.pocket;
import java.time.LocalDate;
import java.util.*;

/** Pure validation applied to a whole merged ledger, not only individual entries.
 * Concurrent devices may each make a valid repayment that is invalid in combination.
 * Reject the merged ledger for review rather than accepting a negative debt balance.
 */
public final class LedgerIntegrity {
 private LedgerIntegrity(){}
 private static final Set<String> TYPES=new HashSet<>(Arrays.asList("expense","income","transfer","lent","borrowed","repay_in","repay_out"));
 private static final Set<String> ACCOUNTS=new HashSet<>(Arrays.asList("Cash","Bank","Wallet"));
 public static final class RepaymentConflict extends IllegalArgumentException {public final String loanId;public final List<String> repayments;public final long principal,total;RepaymentConflict(String loanId,Collection<String> repayments,long principal,long total){super("Combined repayments exceed principal. Review the payments before syncing.");this.loanId=loanId;this.repayments=Collections.unmodifiableList(new ArrayList<>(repayments));this.principal=principal;this.total=total;}}
 public static final class Entry {
  public final String id,kind,day,account,destination,person,loan;
  public final long amount;
  public Entry(String id,String kind,long amount,String day,String account,String destination,String person,String loan){
   this.id=id;this.kind=kind;this.amount=amount;this.day=day;this.account=account;this.destination=destination;this.person=person;this.loan=loan;
  }
 }
 public static void validate(Collection<Entry> entries){validate(entries,ACCOUNTS);}
 public static void validate(Collection<Entry> entries,Set<String> accounts){
  Map<String,Entry> byId=new HashMap<>();Map<String,Long> repaid=new HashMap<>();
  if(entries.size()>100000)throw new IllegalArgumentException("Ledger is too large for this version");
  for(Entry e:entries){
   Identifiers.uuid(e.id);if(byId.put(e.id,e)!=null)throw new IllegalArgumentException("Duplicate entry ID");
   if(!TYPES.contains(e.kind)||e.amount<=0||e.amount>999999999999L)throw new IllegalArgumentException("Invalid entry type or amount");
   if(e.day==null||!e.day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new IllegalArgumentException("Invalid entry date");LocalDate.parse(e.day);
   if(!accounts.contains(e.account))throw new IllegalArgumentException("Invalid account");
   if(e.person==null||e.person.length()>80||e.loan==null||e.destination==null)throw new IllegalArgumentException("Invalid entry details");
   if(e.kind.equals("transfer")){
    if(!accounts.contains(e.destination)||e.account.equals(e.destination))throw new IllegalArgumentException("Invalid transfer accounts");
   }else if(!e.destination.isEmpty())throw new IllegalArgumentException("Unexpected destination account");
   boolean debt=e.kind.equals("lent")||e.kind.equals("borrowed")||e.kind.startsWith("repay_");
   if(debt&&e.person.trim().isEmpty())throw new IllegalArgumentException("Debt requires a person");
   if(!debt&&!e.person.isEmpty())throw new IllegalArgumentException("Unexpected person on non-debt entry");
   if(!e.kind.startsWith("repay_")&&!e.loan.isEmpty())throw new IllegalArgumentException("Unexpected debt link");
  }
  for(Entry e:entries){if(!e.kind.startsWith("repay_"))continue;
   Entry loan=byId.get(e.loan);
   if(loan==null||!(loan.kind.equals("lent")||loan.kind.equals("borrowed")))throw new IllegalArgumentException("Original debt is missing");
   if(!e.kind.equals(loan.kind.equals("lent")?"repay_in":"repay_out"))throw new IllegalArgumentException("Repayment direction differs from debt");
   if(!loan.person.equals(e.person))throw new IllegalArgumentException("Repayment person differs from debt");
   if(e.day.compareTo(loan.day)<0)throw new IllegalArgumentException("Repayment precedes original debt");
   long paid=Math.addExact(repaid.getOrDefault(e.loan,0L),e.amount);repaid.put(e.loan,paid);

  }
  for(String loanId:new TreeSet<>(repaid.keySet())){Entry loan=byId.get(loanId);long total=repaid.get(loanId);if(total>loan.amount){List<String> payments=new ArrayList<>();for(Entry e:entries)if(e.loan.equals(loanId)&&e.kind.startsWith("repay_"))payments.add(e.id);Collections.sort(payments);throw new RepaymentConflict(loanId,payments,loan.amount,total);}}
 }
}
