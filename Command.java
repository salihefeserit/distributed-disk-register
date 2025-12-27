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