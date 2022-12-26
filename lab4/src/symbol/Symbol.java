package symbol;

import type.Type;

/**
 * 一个符号有名称和类型这两个属性
 */
public interface Symbol {

    String getName();

    public Type getType();
}
