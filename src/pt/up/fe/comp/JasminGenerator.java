package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Type;
import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.ClassType;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.Element;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.util.LinkedList;

public class JasminGenerator {

    private final ClassUnit classUnit;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
    }

    public String getFullyQualifiedName(String className) {
        if (className == null) {
            return "java/lang/Object";
        }
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

    private String concatenateStrings(List<String> strings, String delim) {
        StringBuilder builder = new StringBuilder();
        for (String str : strings) {
            builder.append(str);
            builder.append(delim);
        }
        builder.append("\n");
        return builder.toString();
    }

    private String concatenateStrings(List<String> strings) {
        return this.concatenateStrings(strings, " ");
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
                return getCode((AssignInstruction) instruction);
            case CALL:
                return getCode((CallInstruction) instruction);
            default:
                throw new NotImplementedException("Not implemented instruction " + instruction.getInstType());
        }
    }

    private String getCode(CallInstruction method) {
        switch (method.getInvocationType()) {
            case invokestatic:
                return this.getCodeInvokeStatic(method);
            case invokespecial:
                return this.getCodeInvokeSpecial(method);
            default:
                throw new NotImplementedException(method.getInvocationType());
        }
    }

    private String getCodeInvokeStatic(CallInstruction method) {
        String methodClass = ((Operand) method.getFirstArg()).getName();
        String methodName = ((LiteralElement) method.getSecondArg()).getLiteral();
        methodName = methodName.substring(1, methodName.length() - 1);
        return this.concatenateStrings(Arrays.asList(
            "\tinvokestatic ",
            this.getFullyQualifiedName(methodClass), "/", methodName,
            "(", method.getListOfOperands().stream().map(element -> this.getArgumentCode(element)).collect(Collectors.joining()), ")",
            this.getJasminType(method.getReturnType())
        ), "");
    }

    private String getCodeInvokeSpecial(CallInstruction method) {
        throw new NotImplementedException();
    }

    public JasminResult convert() {
        //classUnit.buildVarTables();
        //classUnit.show();
        
        System.out.println(this.convertClass());
        System.out.println(this.convertFields());
        System.out.println(this.convertMethods());
        
        return null;
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
        String classDeclaration = this.concatenateStrings(Arrays.asList(
            ".class public",
            this.classUnit.getClassName()            
        ));

        String superClass = this.getFullyQualifiedName(classUnit.getSuperClass());
        String superDeclaration = this.concatenateStrings(Arrays.asList(
            ".super",
            superClass
        ));

        String initializerDeclaration = this.concatenateStrings(Arrays.asList(
            ".method public <init>()V\n\taload_0\n\tinvokenonvirtual ",
            superClass,
            "/<init>()V\n\treturn\n.end method"
        ), "");

        return classDeclaration + superDeclaration + initializerDeclaration;
    }

    private String convertFields() {
        /*
        .field <access-spec> <field-name> <descriptor> [ = <value> ]
        */
        List<String> fields = new LinkedList<String>();
        for (Field field : this.classUnit.getFields()) {
            String fieldDeclaration = this.concatenateStrings(Arrays.asList(
                ".field",
                field.getFieldAccessModifier().name().toLowerCase(),
                field.getFieldName(),
                this.getJasminType(field.getFieldType())
            ));
            fields.add(fieldDeclaration);
        }
        
        return this.concatenateStrings(fields, "");
    }

    private String convertMethods() {
        List<String> methods = new LinkedList<String>();
        for (Method method : this.classUnit.getMethods()) {
            method.getVarTable();
            String methodName = method.getMethodName();
            String methodDeclaration = this.concatenateStrings(Arrays.asList(
                ".method public ", method.isStaticMethod() ? "static " : "", methodName,
                "(", method.getParams().stream().map(element -> this.getJasminType(element.getType())).collect(Collectors.joining()), ")",
                this.getJasminType(method.getReturnType()), "\n",
                "\t.limit stack 99\n",
                "\t.limit locals 99\n",
                method.getInstructions().stream().map(instruction -> this.getCode(instruction)).collect(Collectors.joining()),
                "\treturn\n",
                ".end method"
            ), "");
            methods.add(methodDeclaration);
        }
        
        return this.concatenateStrings(methods, "");
    }
}