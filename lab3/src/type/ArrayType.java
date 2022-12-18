package type;

import java.util.Objects;

public class ArrayType implements Type {
    Integer numberOfElement;

    Type elementType;

    public ArrayType(Integer numberOfElement, Type elementType) {
        this.elementType = elementType;
        this.numberOfElement = numberOfElement;
    }

    public Integer getNumberOfElement() {
        return numberOfElement;
    }

    public Type getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return "array(" + elementType.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return elementType.equals(arrayType.elementType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementType);
    }
}
