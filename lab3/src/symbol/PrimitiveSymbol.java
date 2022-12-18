package symbol;

import jdk.jshell.spi.SPIResolutionException;
import type.Primitive;
import type.Type;

public class PrimitiveSymbol extends BaseSymbol implements Type  {
    public PrimitiveSymbol(String name) {
        super(name, null);
        Primitive primitive = null;
        switch (name) {
            case "void":
                primitive = new Primitive(Primitive.PrimitiveType.VOID);
                break;
            case "int":
                primitive = new Primitive(Primitive.PrimitiveType.INT);
                break;
            case "float":
                primitive = new Primitive(Primitive.PrimitiveType.FLOAT);
                break;
        }
        type = primitive;
    }

    @Override
    public String toString() {
        return name;
    }
}
