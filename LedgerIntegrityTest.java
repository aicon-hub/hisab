import in.aicon.pocket.*;import java.util.*;
public class LedgerIntegrityTest {
 static int n;static String loanId="11111111-1111-4111-8111-111111111111";
 static LedgerIntegrity.Entry loan(long amount){return new LedgerIntegrity.Entry(loanId,"lent",amount,"2026-11-11","Cash","","Sample","");}
 static LedgerIntegrity.Entry pay(long amount,String person,String kind,String day){return new LedgerIntegrity.Entry(UUID.randomUUID().toString(),kind,amount,day,"Cash","",person,loanId);}
 static void valid(LedgerIntegrity.Entry... e){LedgerIntegrity.validate(Arrays.asList(e));n++;}
 static void invalid(LedgerIntegrity.Entry... e){try{LedgerIntegrity.validate(Arrays.asList(e));throw new AssertionError("Unexpected valid ledger");}catch(IllegalArgumentException ex){n++;}}
 public static void main(String[] args){
  for(int i=1;i<=10000;i++){valid(loan(i),pay(i,"Sample","repay_in","2026-11-11"));invalid(loan(i),pay(i,"Sample","repay_in","2026-11-11"),pay(1,"Sample","repay_in","2026-11-11"));}
  invalid(pay(1,"Sample","repay_in","2026-11-11"));invalid(loan(10),pay(1,"Other","repay_in","2026-11-11"));invalid(loan(10),pay(1,"Sample","repay_out","2026-11-11"));invalid(loan(10),pay(1,"Sample","repay_in","2026-11-10"));invalid(loan(0));invalid(loan(1),loan(1));
  valid(new LedgerIntegrity.Entry(UUID.randomUUID().toString(),"transfer",100,"2026-11-11","Cash","Bank","",""));
  invalid(new LedgerIntegrity.Entry(UUID.randomUUID().toString(),"transfer",100,"2026-11-11","Cash","Cash","",""));
  System.out.println(n+" whole-ledger/debt integrity cases passed");
 }
}
