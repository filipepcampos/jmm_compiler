package pt.up.fe.comp.jasmin;

import pt.up.fe.comp.jmm.jasmin.JasminResult;

import java.io.IOError;
import java.io.IOException;
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

        // constructor
        result.append(".method public <init>()V\n\taload_0\n\tinvokenonvirtual " + this.getSuperClassName() + "/<init>()V\n\treturn\n.end method\n");

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
        String modifier = field.getFieldAccessModifier().name().toLowerCase();
        result.append(modifier.equals("default") ? "private" : modifier).append(" ");
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

        if (method.isConstructMethod()) return "";

        StringBuilder result = new StringBuilder();

        // method header
        result.append(this.convertMethodHeader(method));

        // method limits
        result.append(this.convertMethodLimits(method));

        // method instructions
        method.buildVarTable();
        /*
        try {
            method.outputCFG();
        } catch (OllirErrorException e) {
            e.printStackTrace();
        }*/
        Instruction lastInstruction = null;
        for (Instruction instruction : method.getInstructions()) {
            result.append(this.getCode(instruction, method.getVarTable(), method.getLabels(instruction)));
            lastInstruction = instruction;
        }

        // append return if method return type is void
        if (lastInstruction == null || (lastInstruction.getInstType() != InstructionType.RETURN && method.getReturnType().getTypeOfElement() == ElementType.VOID)) {
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

        /* if (method.isConstructMethod()) {
            result.append("<init>");    // TODO: Doubt
        } else {
            result.append(method.getMethodName());
        } */

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
        int localsSize = method.getVarTable().size() + (method.isStaticMethod() || method.getVarTable().containsKey("this") ? 0 : 1);
        return "\t.limit stack 99\n\t.limit locals " + localsSize + "\n";
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

        throw new RuntimeException("getFullyQualifiedName: Could not find import for class " + className);
    }

    private String getJasminType(Type type) {

        StringBuilder result = new StringBuilder();

        ElementType elementType = type.getTypeOfElement();
        if (elementType == ElementType.ARRAYREF) {
            result.append("[".repeat(((ArrayType) type).getNumDimensions()));
            elementType = ((ArrayType) type).getArrayType();
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
            case ARRAYREF:
            case CLASS:
            case THIS:
                throw new RuntimeException("getJasminType: Unrecognized " + elementType + " element type");
        }

        return result.toString();
    }

    private String getElementClass(Element element) {

        ElementType type = element.getType().getTypeOfElement();

        switch (type) {
            case THIS:
                return this.classUnit.getClassName();
            case OBJECTREF:
                return this.getFullyQualifiedName(((ClassType) element.getType()).getName());
            case CLASS:
                return ((Operand) element).getName();
            default:
                throw new RuntimeException("getElementClass: Unrecognized " + type + " element type");
        }
    }

    private String getCode(Instruction instruction, HashMap<String, Descriptor> varTable, List<String> labels) {

        //System.out.println(instruction.getInstType());  // DEBUG

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
                break;
            case PUTFIELD:
                result.append(this.getCode((PutFieldInstruction) instruction, varTable));
                break;
            case UNARYOPER:
                result.append(this.getCode((UnaryOpInstruction) instruction, varTable));
                break;
            case BINARYOPER:
                result.append(this.getCode((BinaryOpInstruction) instruction, varTable));
                break;
            case NOPER:
                result.append(this.getCode((SingleOpInstruction) instruction, varTable));
                break;
        }

        return result.toString();
    }

    private String getCode(AssignInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        Operand operand = (Operand) instruction.getDest();
        if (operand instanceof ArrayOperand) {
            result.append("\taload ").append(varTable.get(operand.getName()).getVirtualReg()).append("\n");
            result.append(this.loadElement(((ArrayOperand) operand).getIndexOperands().get(0), varTable));  // TODO: Support for multiple dimensional arrays
        }

        // deal with value of right hand side of instruction first
        result.append(this.getCode(instruction.getRhs(), varTable, null));
        
        // store the value
        result.append(this.storeElement(operand, varTable));

        return result.toString();
    }

    private String getCode(CallInstruction method, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        switch (method.getInvocationType()) {
            case invokevirtual:
            case invokeinterface:
            case invokestatic:
            case invokespecial:
                result.append(this.getInvokeCode(method, varTable));
                break;
            case NEW:
                result.append(this.getNewCode(method, varTable));
                break;
            case arraylength:
                result.append(this.loadElement(method.getFirstArg(), varTable)).append("\tarraylength\n");
                break;
            case ldc:
                result.append(this.loadElement(method.getFirstArg(), varTable));
                break;
        }

        return result.toString();
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

        ElementType type = instruction.getOperand().getType().getTypeOfElement();

        switch (type) {
            case VOID:
                return "\treturn\n";
            case INT32:
            case BOOLEAN:
                return this.loadElement(instruction.getOperand(), varTable) + "\tireturn\n";
            case ARRAYREF:
            case OBJECTREF:
                return this.loadElement(instruction.getOperand(), varTable) + "\tareturn\n";
            default:
                throw new RuntimeException("getCode: Unrecognized return instruction for " + type + " element type");
        }
    }

    private String getCode(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getFirstOperand(), varTable));
        result.append("\tgetfield ");
        result.append(this.getElementClass(instruction.getFirstOperand())).append("/");
        result.append(((Operand) instruction.getSecondOperand()).getName()).append(" ");
        result.append(this.getJasminType(((Operand) instruction.getSecondOperand()).getType())).append("\n");

        return result.toString();
    }

    private String getCode(PutFieldInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getFirstOperand(), varTable));
        result.append(this.loadElement(instruction.getThirdOperand(), varTable));
        result.append("\tputfield ");
        result.append(this.getElementClass(instruction.getFirstOperand())).append("/");
        result.append(((Operand) instruction.getSecondOperand()).getName()).append(" ");
        result.append(this.getJasminType(((Operand) instruction.getSecondOperand()).getType())).append("\n");

        return result.toString();
    }

    private String getCode(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getOperand(), varTable));
        result.append("\ticonst_1\nixor\n");

        return result.toString();
    }

    private String getCode(BinaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getLeftOperand(), varTable));
        result.append(this.loadElement(instruction.getRightOperand(), varTable));

        OperationType opType = instruction.getOperation().getOpType();

        switch (opType) {
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
                result.append("\ticonst_0\n");
                result.append("\tgoto ").append(this.getCondFalseLabel()).append("\n");
                result.append(this.getCondTrueLabel()).append(":\n");
                result.append("\ticonst_1\n");
                result.append(this.getCondFalseLabel()).append(":\n");
                this.numberCond++;
                break;
            case ANDB:
                result.append("\tiand\n");
                break;
            case NOTB:
                break;
            default:
                throw new RuntimeException("getCode: Unrecognized binary operation for " + opType + " operation type");
        }

        return result.toString();
    }

    private String getCode(SingleOpInstruction instruction, HashMap<String, Descriptor> varTable) {
        return this.loadElement(instruction.getSingleOperand(), varTable);
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
            case STRING:
            case ARRAYREF:
                return "\tastore " + varTable.get(operand.getName()).getVirtualReg() + "\n";
            default:
                throw new RuntimeException("storeElement: Unrecognized operand type " + operand.getType());
        }
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        if (element instanceof LiteralElement) {
            result.append("\tldc ").append(((LiteralElement) element).getLiteral()).append("\n");
        } else if (element instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) element;
            result.append("\taload ").append(varTable.get(arrayOperand.getName()).getVirtualReg()).append("\n");
            result.append(this.loadElement(arrayOperand.getIndexOperands().get(0), varTable)); // TODO: Support for multiple dimensional arrays
            result.append("\tiaload\n");
        } else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            ElementType type = operand.getType().getTypeOfElement();
            switch (type) {
                case THIS:
                    result.append("\taload_0\n");
                    break;
                case INT32:
                case BOOLEAN:
                    result.append("\tiload ").append(varTable.get(operand.getName()).getVirtualReg()).append("\n");
                    break;
                case OBJECTREF:
                case ARRAYREF:
                case STRING:
                    result.append("\taload ").append(varTable.get(operand.getName()).getVirtualReg()).append("\n");
                    break;
                case CLASS:     // this happens in invokestatic
                    break;
                case VOID:
                    throw new RuntimeException("loadElement: Unrecognized load instruction for " + type + " element type");
            }
        }
        
        return result.toString();
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

    private String getNewCode(CallInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        ElementType type = instruction.getFirstArg().getType().getTypeOfElement();

        switch (type) {
            case ARRAYREF:
                int size = 0;
                for (Element element : instruction.getListOfOperands()) {
                    result.append(this.loadElement(element, varTable));
                    size++;
                }
                result.append("\tmultianewarray ").append(this.getJasminType(instruction.getReturnType())).append(" ").append(size).append("\n");
                break;
            case OBJECTREF:
                result.append("\tnew ").append(((Operand) instruction.getFirstArg()).getName()).append("\n");
                break;
            default:
                throw new RuntimeException("getNewCode: Unrecognized new instruction for " + type + " element type");
        }

        return result.toString();
    }
}