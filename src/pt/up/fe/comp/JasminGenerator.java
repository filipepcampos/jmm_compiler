package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

public class JasminGenerator {

    private final ClassUnit classUnit;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.reset();
    }

    public String getFullyQualifiedName(String className) {
        for (String importString : this.classUnit.getImports()) {
            if (importString.endsWith(className)) {
                return importString.replace('.', '/');
            }
        }

        throw new RuntimeException("Could not find import for class " + className);
    }

    public JasminResult convert() {
        this.reset();
        classUnit.buildVarTables();
        classUnit.show();
        
        System.out.println(this.convertClass());
        System.out.println(this.convertFields());
        
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

    private String convertMethods(ClassUnit classUnit) {
        return null;
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

    private void reset() {}
}