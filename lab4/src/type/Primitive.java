package type;

import java.util.Objects;

public class Primitive implements Type {
    public enum PrimitiveType {
        INT, FLOAT, VOID
    }

    PrimitiveType type;

    public Primitive(PrimitiveType type) {
        this.type = type;
    }

    public PrimitiveType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Primitive primitive = (Primitive) o;
        return type.name().equals(primitive.type.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}
