package pt.up.fe.comp;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.List;

import org.specs.comp.ollir.*;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private String superClass;
    private int numberCond;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.superClass = null;
        this.numberCond = 0;
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
        StringBuilder result = new StringBuilder();

        for (Field field : this.classUnit.getFields()) {
            result.append(this.convertField(field));
        }
        
        return result.toString();
    }

    private String convertField(Field field) {
        /*
        .field <access-spec> <field-name> <descriptor> [ = <value> ]
        */

        StringBuilder result = new StringBuilder();

        result.append(".field ");
        result.append(field.getFieldAccessModifier().name().toLowerCase()).append(" ");
        result.append(field.getFieldName()).append(" ");
        result.append(this.getJasminType(field.getFieldType())).append("\n");

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
        method.buildVarTable();
        Instruction lastInstruction = null;
        for (Instruction instruction : method.getInstructions()) {
            result.append(this.getCode(instruction, method.getVarTable(), method.getLabels(instruction)));
            lastInstruction = instruction;
        }

        // append return if method return type is void
        if (lastInstruction != null && lastInstruction.getInstType() != InstructionType.RETURN && method.getReturnType().getTypeOfElement() == ElementType.VOID) {
            result.append("\treturn\n");
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

        if (method.isFinalMethod()) {   // NOTE: Not needed regarding our grammar
            result.append("final ");
        }

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
        if (className.equals(this.classUnit.getClassName())) {
            return this.classUnit.getClassName();
        }

        for (String importString : this.classUnit.getImports()) {
            if (importString.endsWith(className)) {
                return importString.replace('.', '/');
            }
        }

        throw new RuntimeException("Could not find import for class " + className);
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
                throw new RuntimeException("Unrecognized type " + type);
        }

        return result.toString();
    }

    private String getElementClass(Element element) {
        switch (element.getType().getTypeOfElement()) {
            case THIS:
                return this.classUnit.getClassName();
            case OBJECTREF:
                return this.getFullyQualifiedName(((ClassType) element.getType()).getName());
            case CLASS:
                return ((Operand) element).getName();
            default:
                throw new RuntimeException("Unrecognized element type " + element.getType());
        }
    }

    private String getCode(Instruction instruction, HashMap<String, Descriptor> varTable, List<String> labels) {

        System.out.println(instruction.getInstType());  // DEBUG

        StringBuilder result = new StringBuilder();

        if (labels != null) {
            for (String label : labels) {
                result.append(label).append(":\n");
            }
        }

        switch (instruction.getInstType()) {
            case ASSIGN:
                result.append(this.getCode((AssignInstruction) instruction, varTable));
                break;
            case CALL:
                result.append(this.getCode((CallInstruction) instruction, varTable));
                break;
            case GOTO:
                result.append(this.getCode((GotoInstruction) instruction, varTable));
                break;
            case BRANCH:
                result.append(this.getCode((CondBranchInstruction) instruction, varTable));
                break;
            case RETURN:
                result.append(this.getCode((ReturnInstruction) instruction, varTable));
                break;
            case GETFIELD:
                result.append(this.getCode((GetFieldInstruction) instruction, varTable));
            case UNARYOPER:
                throw new NotImplementedException(instruction.getInstType());
            case BINARYOPER:
                result.append(this.getCode((BinaryOpInstruction) instruction, varTable));
                break;
            case NOPER:
                result.append(this.loadElement(((SingleOpInstruction) instruction).getSingleOperand(), varTable));
                break;
            default:
                throw new RuntimeException("Unrecognized instruction " + instruction.getInstType());
        }

        return result.toString();
    }

    private String getCode(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder result = new StringBuilder();
        Operand operand = (Operand) instruction.getDest();

        /* if (operand instanceof ArrayOperand) {   // TODO
            ArrayOperand aoperand = (ArrayOperand) operand;
        } */

        // deal with value of right hand side of instruction first
        result.append(this.getCode(instruction.getRhs(), varTable, null));
        
        // store the value (if needed)
        result.append(this.storeElement(operand, varTable));

        return result.toString();
    }

    private String getCode(CallInstruction method, HashMap<String, Descriptor> varTable) {
        switch (method.getInvocationType()) {
            case invokevirtual:
            case invokeinterface:
            case invokestatic:
            case invokespecial:
                return this.getInvokeCode(method, varTable);
            case NEW:
                return "\tnew " + ((Operand) method.getFirstArg()).getName() + "\n";
            default:
                throw new RuntimeException("Unrecognized call instruction " + method.getInvocationType());
        }
    }

    private String getCode(GotoInstruction instruction, HashMap<String, Descriptor> varTable) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    private String getCode(CondBranchInstruction instruction, HashMap<String, Descriptor> varTable) {
        StringBuilder result = new StringBuilder();

        result.append(this.getCode(instruction.getCondition(), varTable, null));
        result.append("\tifeq ").append(instruction.getLabel()).append("\n");

        return result.toString();
    }

    private String getCode(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {
        if (!instruction.hasReturnValue()) {
            return "\treturn\n";
        }

        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case VOID:
                return "\treturn\n";
            case INT32:
            case BOOLEAN:
                return this.loadElement(instruction.getOperand(), varTable) + "\tireturn\n";
            case ARRAYREF:
            case OBJECTREF:
                return this.loadElement(instruction.getOperand(), varTable) + "\tareturn\n";
            default:
                throw new RuntimeException("Unrecognized return instruction for " + instruction.getOperand().getType());
        }
    }

    private String getCode(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getFirstOperand(), varTable));
        result.append("getfield ");
        result.append(this.getElementClass(instruction.getFirstOperand())).append("/");
        result.append(((Operand) instruction.getSecondOperand()).getName()).append(" ");
        result.append(this.getJasminType(((Operand) instruction.getSecondOperand()).getType()));

        return result.toString();
    }

    private String getCode(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getLeftOperand(), varTable));
        result.append(this.loadElement(instruction.getRightOperand(), varTable));

        switch (instruction.getOperation().getOpType()) {
            case ADD:
                result.append("\tiadd\n");
                break;
            case SUB:
                result.append("\tisub\n");
                break;
            case MUL:
                result.append("\timul\n");
                break;
            case DIV:
                result.append("\tidiv\n");
                break;
            case LTH:
                result.append("\tif_icmplt ").append(this.getCondTrueLabel()).append("\n");
                result.append("\ticonst_1\n");
                result.append("\tgoto ").append(this.getCondFalseLabel()).append("\n");
                result.append(this.getCondTrueLabel()).append(":\n");
                result.append("\ticonst_0\n");
                result.append(this.getCondFalseLabel()).append(":\n");
                this.numberCond++;
                break;
            case ANDB:
                result.append("\tiand\n");
                break;
            case NOTB:
                result.append("\tineg\n");
                break;
            default:
                throw new RuntimeException("Unrecognized binary operation " + instruction.getOperation());
        }

        return result.toString();
    }

    private String getCondTrueLabel() {
        return "true" + this.numberCond;
    }

    private String getCondFalseLabel() {
        return "false" + this.numberCond;
    }

    private String storeElement(Operand operand, HashMap<String, Descriptor> varTable) {
        if (operand instanceof ArrayOperand) {
            return "\tiastore\n";
        }

        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                return "\tistore " + varTable.get(operand.getName()).getVirtualReg() + "\n";
            case OBJECTREF:
            case ARRAYREF:
                return "\tastore " + varTable.get(operand.getName()).getVirtualReg() + "\n";
            default:
                throw new RuntimeException("Unrecognized operand type " + operand.getType());
        }
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            return "\tldc " + ((LiteralElement) element).getLiteral() + "\n";
        }

        if (element instanceof Operand) {
            Operand operand = (Operand) element;
            switch (operand.getType().getTypeOfElement()) {
                case THIS:
                    return "\taload_0\n";
                case INT32:
                case BOOLEAN:
                    return "\tiload "  + varTable.get(operand.getName()).getVirtualReg() + "\n";
                case OBJECTREF:
                case ARRAYREF:
                    return "\taload "  + varTable.get(operand.getName()).getVirtualReg() + "\n";
                case CLASS:
                    return "";
                default:
                    throw new RuntimeException("Unrecognized operand type " + operand.getType());
            }
        }

        throw new RuntimeException("Unrecognized Element instance " + element.getClass());
    }

    private String getInvokeCode(CallInstruction method, HashMap<String, Descriptor> varTable) {
        /*
        invokestatic io/println(Ljava/lang/String;)V
        */

        //method.show();  // DEBUG

        StringBuilder result = new StringBuilder();

        // load object
        result.append(this.loadElement(method.getFirstArg(), varTable));

        // load arguments
        for (Element param : method.getListOfOperands()) {
            result.append(this.loadElement(param, varTable));
        }

        // append invocation type
        result.append("\t").append(method.getInvocationType()).append(" ");

        // obtain class name
        String methodClass = this.getElementClass(method.getFirstArg());

        // obtain method name
        String methodName = ((LiteralElement) method.getSecondArg()).getLiteral();
        methodName = methodName.substring(1, methodName.length() - 1);

        // append method class and name
        result.append(methodClass).append("/").append(methodName);

        // append argument types
        result.append("(");
        for (Element param : method.getListOfOperands()) {
            result.append(this.getJasminType(param.getType()));
        }
        result.append(")");

        // append return type
        result.append(this.getJasminType(method.getReturnType())).append("\n");

        return result.toString();
    }
}