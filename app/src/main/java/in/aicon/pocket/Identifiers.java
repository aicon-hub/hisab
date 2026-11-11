package in.aicon.pocket;
import java.util.UUID;
/** Canonical record IDs only. Java's UUID parser also accepts shortened spellings. */
public final class Identifiers {
 private Identifiers(){}
 public static UUID uuid(String value){if(value==null||!value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException("Invalid canonical record ID");return UUID.fromString(value);}
}
