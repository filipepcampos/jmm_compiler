package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Type;
import org.specs.comp.ollir.ArrayType;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Operand;
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
        for (String importString : this.classUnit.getImports()) {
            if (importString.endsWith(className)) {
                return importString.replace('.', '/');
            }
        }
        throw new RuntimeException("Could not find import for class " + className);
    }

    private String concatenateStrings(List<String> strings) {
        StringBuilder builder = new StringBuilder();
        for (String str : strings) {
            builder.append(str);
            builder.append(" ");
        }
        builder.append("\n");
        return builder.toString();
    }

    private String getJasminType(Type type) {
        if (type instanceof ArrayType) {
            return this.getJasminType((ArrayType) type);
        }
        return getJasminType(type.getTypeOfElement());
    }

    private String getJasminType(ArrayType type) {
        return "[" + getJasminType(type.getTypeOfElements());
    }

    private String getJasminType(ElementType type) {
        switch (type) {
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z"; // TODO: Complete with other types
            default:
                throw new NotImplementedException("Not implemented type " + type);
        }
    }

    private String getCode(Instruction method) {
        FunctionClassMap<Instruction, String> instructionMap = new FunctionClassMap<>();
        
        instructionMap.put(CallInstruction.class, this::getCode);
        instructionMap.put(CallInstruction.class, this::getCode);

        instructionMap.apply(method);

        throw new NotImplementedException(method.getClass());
    }

    private String getCode(CallInstruction method) {
        switch (method.getInvocationType()) {
            case invokestatic:
                return this.getCodeInvokeStatic(method);
            default:
                throw new NotImplementedException(method.getInvocationType());
        }
    }

    private String getCodeInvokeStatic(CallInstruction method) {
        String methodClass = ((Operand) method.getFirstArg()).getName();
        String methodName = ((LiteralElement) method.getSecondArg()).getLiteral();
        return this.concatenateStrings(Arrays.asList(
            "invokestatic",
            this.getFullyQualifiedName(methodClass),
            "/",
            methodName,
            "(",    // TODO: Arguments
            ")",
            this.getJasminType(method.getReturnType())
        ));
    }

    public JasminResult convert() {
        //classUnit.buildVarTables();
        //classUnit.show();
        
        //System.out.println(this.convertClass());
        //System.out.println(this.convertFields());
        System.out.println(this.convertMethods());
        
        return null;
    }

    private String convertClass() {
        /*
        .class public HelloWorld
        .super java/lang/Object
        */
        String classDeclaration = this.concatenateStrings(Arrays.asList(
            ".class public",
            this.classUnit.getClassName()            
        ));

        String superClass = classUnit.getSuperClass();
        String superDeclaration = this.concatenateStrings(Arrays.asList(
            ".super",
            superClass == null ? "java/lang/Object" : this.getFullyQualifiedName(superClass)
        ));

        return classDeclaration + superDeclaration;
    }

    private String convertFields() {
        /*
        .field <access-spec> <field-name> <descriptor> [ = <value> ]
        */
        List<String> fields = new LinkedList<String>();
        for (Field field : this.classUnit.getFields()) {
            String fieldDeclaration = this.concatenateStrings(Arrays.asList(
                ".field",
                field.getFieldAccessModifier().name(),
                field.getFieldName(),
                field.getFieldType().toString()
                // TODO: GET INITIAL VALUE
            ));
            fields.add(fieldDeclaration);
        }
        
        return this.concatenateStrings(fields);
    }

    private String convertMethods() {
        List<String> methods = new LinkedList<String>();
        for (Method method : this.classUnit.getMethods()) {
            String methodAccess = method.getMethodAccessModifier().name().toLowerCase();
            String methodDeclaration = this.concatenateStrings(Arrays.asList(
                ".method",
                methodAccess == "default" ? "" : methodAccess,
                method.isStaticMethod() ? "static" : "",
                method.getParams().stream().map(element -> this.getJasminType(element.getType())).collect(Collectors.joining()),
                this.getJasminType(method.getReturnType()),
                ".limit stack 99\n",
                ".limit locals 99\n",
                "return\n",
                ".end method"
            ));
            methods.add(methodDeclaration);
        }
        
        return this.concatenateStrings(methods);
    }
}