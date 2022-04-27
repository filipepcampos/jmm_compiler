package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

public class JasminGenerator implements JasminBackend {

    public JasminGenerator() {
        this.reset();
    }

    public JasminResult toJasmin(OllirResult ollirResult) {
        this.reset();
        ClassUnit classUnit = ollirResult.getOllirClass();
        classUnit.buildVarTables();
        classUnit.show();
        
        System.out.println(this.convertClass(classUnit));
        System.out.println(this.convertFields(classUnit));
        
        return null;
    }

    private String convertClass(ClassUnit classUnit) {
        /*
        .class public HelloWorld
        .super java/lang/Object
        */
        String classDeclaration = this.concatenateStrings(Arrays.asList(
            ".class",
            classUnit.getClassAccessModifier().name(),
            classUnit.getClassName()            
        ));

        String superClass = classUnit.getSuperClass();
        String superDeclaration = this.concatenateStrings(Arrays.asList(
            ".super",
            superClass == null ? "java/lang/Object" : superClass
        ));

        return classDeclaration + superDeclaration;
    }

    private String convertFields(ClassUnit classUnit) {
        /*
        .field <access-spec> <field-name> <descriptor> [ = <value> ]
        */
        List<String> fields = new LinkedList<String>();
        for (Field field : classUnit.getFields()) {
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