import in.aicon.pocket.AccountScope;
public class AccountScopeTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError(n);}
 public static void main(String[] args){AccountScope s=new AccountScope();try{s.ticket();throw new AssertionError();}catch(IllegalStateException expected){n++;}
 for(int i=1;i<=10000;i++){
 String a=""+i,b=""+(i+10000);s.bind(a);AccountScope.Ticket x=s.ticket();ok(s.isCurrent(x));ok(!x.database.equals(AccountScope.LOCAL_DATABASE));ok(!x.database.equals(AccountScope.databaseFor(b)));s.bind(b);ok(!s.isCurrent(x));AccountScope.Ticket y=s.ticket();ok(s.isCurrent(y));s.disconnect();ok(!s.isCurrent(y));s.bind(a);ok(!s.isCurrent(x));AccountScope.Ticket z=s.ticket();s.bind(a);ok(!s.isCurrent(z));
 }
 AccountScope other=new AccountScope();other.bind("123");s.bind("123");ok(!s.isCurrent(other.ticket()));
 for(String invalid:new String[]{null,"","aicon4tech@gmail.com","../ledger","123/456"}){try{AccountScope.databaseFor(invalid);throw new AssertionError();}catch(IllegalArgumentException expected){n++;}}
 System.out.println(n+" account-partition/in-flight invalidation assertions passed");}
}
