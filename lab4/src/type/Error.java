package type;

public class Error implements Type {
    public static String errorMessageTemplate = "Error type %d at Line %d: %s" + System.lineSeparator();

    public static int errorCount;

    public enum ErrorType {
        undeclaredVariable,
        undefinedFunction,
        redefinedVariable,
        redefinedFunction,
        incompatibleAssign,
        incompatibleOperation,
        incompatibleReturnValue,
        incompatibleParameters,
        variableNotAddressable,
        variableNotCallable,
        assignToFunction
    }

    public Error() {
        errorCount += 1;
    }
}
