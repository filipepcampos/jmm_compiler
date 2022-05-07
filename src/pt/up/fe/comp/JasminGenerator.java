package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import org.specs.comp.ollir.*;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private String superClass;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public JasminResult convert() {
        StringBuilder result = new StringBuilder();
        result.append(this.convertClass());
        result.append(this.convertMethods());

        System.out.println(result);     // DEBUG
        
        return new JasminResult(result.toString());
    }

    private String convertClass() {
        /*
        .class public HelloWorld
        .super java/lang/Object

        .method public <init>()V
            aload_0
            invokenonvirtual java/lang/Object/<init>()V
            return
        .end method
        */

        StringBuilder result = new StringBuilder();

        // .class directive
        result.append(this.convertClassDirective());

        // .super directive
        result.append(this.convertSuperDirective());

        // class fields
        result.append(this.convertFields());

        return result.toString();
    }

    private String convertClassDirective() {
        return ".class public " + this.classUnit.getClassName() + "\n";
    }

    private String convertSuperDirective() {
        return ".super " + this.getSuperClassName() + "\n";
    }

    private String getSuperClassName() {
        if (this.superClass == null) {
            this.superClass = classUnit.getSuperClass();
            if (this.superClass != null) {
                this.superClass = this.getFullyQualifiedName(superClass);
            } else {
                this.superClass = "java/lang/Object";
            }
        }
        return this.superClass;
    }

    private String convertFields() {
        /*
        .field <access-spec> <field-name> <descriptor> [ = <value> ]
        */

        StringBuilder result = new StringBuilder();

        for (Field field : this.classUnit.getFields()) {
            result.append(".field ");
            result.append(field.getFieldAccessModifier().name().toLowerCase()).append(" ");
            result.append(field.getFieldName()).append(" ");
            result.append(this.getJasminType(field.getFieldType())).append("\n");
        }
        
        return result.toString();
    }

    private String convertMethods() {
        /*
        .method public static main([Ljava/lang/String;)V
            .limit stack 99
            .limit locals 99
            ...
            return
        .end method
        */

        StringBuilder result = new StringBuilder();

        for (Method method : this.classUnit.getMethods()) {
            result.append(this.convertMethod(method));
        }

        return result.toString();
    }

    private String convertMethod(Method method) {
        if (method.isConstructMethod()) {
            return ".method public <init>()V\n\taload_0\n\tinvokenonvirtual " + this.getSuperClassName() + "/<init>()V\n\treturn\n.end method\n";
        }

        StringBuilder result = new StringBuilder();

        // method header
        result.append(this.convertMethodHeader(method));

        // method limits
        result.append(this.convertMethodLimits(method));

        // method instructions
        for (Instruction instruction : method.getInstructions()) {
            result.append(this.getCode(instruction));
        }

        // method end directive
        result.append(".end method\n");

        return result.toString();
    }

    private String convertMethodHeader(Method method) {
        /*
        .method public static main([Ljava/lang/String;)V
        */

        StringBuilder result = new StringBuilder();

        result.append(".method public ");

        if (method.isStaticMethod()) {
            result.append("static ");
        }

        result.append(method.getMethodName());

        result.append("(");
        for (Element param : method.getParams()) {
            result.append(this.getJasminType(param.getType()));
        }
        result.append(")");

        result.append(this.getJasminType(method.getReturnType())).append("\n");

        return result.toString();
    }

    private String convertMethodLimits(Method method) {
        /*
        .limit stack 99
        .limit locals 99
        */

        return "\t.limit stack 99\n\t.limit locals 99\n";
    }

    public String getFullyQualifiedName(String className) {
        for (String importString : this.classUnit.getImports()) {
            if (importString.endsWith(className)) {
                return importString.replace('.', '/');
            }
        }
        throw new RuntimeException("Could not find import for class " + className);
    }

    public String getArgumentCode(Element element) {
        return "";
    }

    private String getJasminType(Type type) {
        ElementType elementType = type.getTypeOfElement();
        StringBuilder result = new StringBuilder();

        if (elementType == ElementType.ARRAYREF) {
            ArrayType arrayType = (ArrayType) type;
            elementType = arrayType.getTypeOfElements();
            result.append("[".repeat(arrayType.getNumDimensions()));
        }

        switch (elementType) {
            case STRING:
                result.append("Ljava/lang/String;");
                break;
            case VOID:
                result.append("V");
                break;
            case INT32:
                result.append("I");
                break;
            case BOOLEAN:
                result.append("Z");
                break;
            case OBJECTREF:
                result.append("L").append(this.getFullyQualifiedName(((ClassType) type).getName())).append(";");
                break;
            default:
                throw new NotImplementedException("Not implemented type " + type);
        }

        return result.toString();
    }

    private String getCode(Instruction instruction) {
        switch (instruction.getInstType()) {
            case ASSIGN:
                return ""; // getCode((AssignInstruction) instruction);
            case CALL:
                return getCode((CallInstruction) instruction);
            case GOTO:
                return "";
            case BRANCH:
                return "";
            case RETURN:
                return getCode((ReturnInstruction) instruction);
            case GETFIELD:
                throw new NotImplementedException(instruction.getInstType());
            case UNARYOPER:
                throw new NotImplementedException(instruction.getInstType());
            case BINARYOPER:
                throw new NotImplementedException(instruction.getInstType());
            case NOPER:
                throw new NotImplementedException(instruction.getInstType());
            default:
                throw new RuntimeException("Unrecognized instruction");
        }
    }

    private String getCode(CallInstruction method) {
        switch (method.getInvocationType()) {
            case invokevirtual:
                throw new NotImplementedException(method.getInvocationType());
            case invokeinterface:
                throw new NotImplementedException(method.getInvocationType());
            case invokespecial:
                return this.getCodeInvokeSpecial(method);
            case invokestatic:
                return this.getCodeInvokeStatic(method);
            case NEW:
                throw new NotImplementedException(method.getInvocationType());
            case arraylength:
                throw new NotImplementedException(method.getInvocationType());
            case ldc:
                throw new NotImplementedException(method.getInvocationType());
            default:
                throw new RuntimeException("Unrecognized call instruction");
        }
    }

    private String getCode(ReturnInstruction instruction) {
        if (!instruction.hasReturnValue()) {
            return "\treturn\n";
        }

        StringBuilder result = new StringBuilder();

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID:
                result.append("\treturn\n");
                break;
            case INT32:
            case BOOLEAN:
                result.append("\tireturn\n");
                break;
            case ARRAYREF:
            case OBJECTREF:
                result.append("\tareturn\n");
                break;
            default:
                break;
        }

        return result.toString();
    }

    private String getCodeInvokeStatic(CallInstruction method) {
        /*
        invokestatic io/println(Ljava/lang/String;)V
        */

        StringBuilder result = new StringBuilder();

        String methodClass = ((Operand) method.getFirstArg()).getName();
        String methodName = ((LiteralElement) method.getSecondArg()).getLiteral();
        methodName = methodName.substring(1, methodName.length() - 1);

        result.append("\tinvokestatic ").append(this.getFullyQualifiedName(methodClass)).append("/").append(methodName);

        result.append("(");
        for (Element param : method.getListOfOperands()) {
            result.append(this.getArgumentCode(param));
        }
        result.append(")");

        result.append(this.getJasminType(method.getReturnType())).append("\n");

        return result.toString();
    }

    private String getCodeInvokeSpecial(CallInstruction method) {
        return "";
    }
}