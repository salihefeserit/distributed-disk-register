public class Command {
    public enum Type {
        SET,
        GET,
        UNKNOWN
    }


private final Type type;
private final String id;
private final String message;

public Command(Type type, String id, String message) {
    this.type = type;
    this.id = id;
    this.message = message;
}

public static Command parse(String line) {
    if (line == null || line.trim().isEmpty()) {
        return new Command(Type.UNKNOWN, null, null);
    }

    String[] parts = line.trim().splt("\\s+", 3);
    String cmd = parts[0].toUpperCase();

    if ("SET".equals(cmd) && parts.length == 3) {
        return new Command(Type.SET, parts[1], parts[2]);
    }

    if ("GET".equals(cmd) && parts.length == 2) {
        return new Command(Type.GET, parts[1], null);
    }

    return new Command(Type.UNKNOWN, null, null);

}

public Type getType() {
    return type;
}

public String getId() {
    return id;
}

public String getMessage() {
    return message;
}
}