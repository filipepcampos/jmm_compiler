package pt.up.fe.comp.jasmin;

import pt.up.fe.comp.jmm.jasmin.JasminResult;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.specs.comp.ollir.*;

public class JasminGenerator {

    private final ClassUnit classUnit;
    private String superClass;
    private int numberCond;
    private StackLimits stackLimits;

    public JasminGenerator(ClassUnit classUnit) {
        this.classUnit = classUnit;
        this.superClass = null;
        this.numberCond = 0;
        this.stackLimits = new StackLimits();
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

        // method header
        String header = this.convertMethodHeader(method);

        // method instructions
        this.stackLimits.reset();
        String instructions = this.convertMethodInstructions(method);
        /* if (!this.stackLimits.isEmpty()) {
            throw new RuntimeException("convertMethod: Stack isn't empty at the end of method declaration");
        } */

        // method limits
        String limits = this.convertMethodLimits(method);

        // method end directive
        String endDirective = ".end method\n";

        return header + limits + instructions + endDirective;
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

    private String convertMethodInstructions(Method method) {

        StringBuilder result = new StringBuilder();

        method.buildVarTable();
        Instruction lastInstruction = null;
        for (Instruction instruction : method.getInstructions()) {
            result.append(this.getCode(instruction, method.getVarTable(), method.getLabels(instruction)));
            lastInstruction = instruction;
        }

        // append return if method return type is void
        if (lastInstruction == null || (lastInstruction.getInstType() != InstructionType.RETURN && method.getReturnType().getTypeOfElement() == ElementType.VOID)) {
            result.append("\treturn\n");
        }

        return result.toString();
    }

    private String convertMethodLimits(Method method) {
        /*
        .limit stack 99
        .limit locals 99
        */

        int stackLimit = this.stackLimits.getMaxStackSize();
        int localsLimit = method.getVarTable().size() + (method.isStaticMethod() || method.getVarTable().containsKey("this") ? 0 : 1);

        return "\t.limit stack " + stackLimit + "\n\t.limit locals " + localsLimit + "\n";
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
        instruction.show();
        System.out.println(this.stackLimits);

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
            result.append("\taload").append(this.getVirtualReg(operand.getName(), varTable)).append("\n");
            this.stackLimits.update(1);
            result.append(this.loadElement(((ArrayOperand) operand).getIndexOperands().get(0), varTable));  // TODO: Support for multiple dimensional arrays
        }

        Instruction rhs = instruction.getRhs();
        
        // iinc
        if (rhs.getInstType() == InstructionType.BINARYOPER &&
            ((BinaryOpInstruction) rhs).getOperation().getOpType() == OperationType.ADD) {
            
            BinaryOpInstruction iincInst = (BinaryOpInstruction) rhs;
            Element leftOperand = iincInst.getLeftOperand();
            Element rightOperand = iincInst.getRightOperand();
            
            if (leftOperand.isLiteral() && !rightOperand.isLiteral() && ((Operand) rightOperand).getName().equals(operand.getName())) {
                result.append("\tiinc ").append(varTable.get(((Operand) rightOperand).getName()).getVirtualReg()).append(" ");
                result.append(((LiteralElement) leftOperand).getLiteral()).append("\n");
                return result.toString();
            }

            if (rightOperand.isLiteral() && !leftOperand.isLiteral() && ((Operand) leftOperand).getName().equals(operand.getName())) {
                result.append("\tiinc ").append(varTable.get(((Operand) leftOperand).getName()).getVirtualReg()).append(" ");
                result.append(((LiteralElement) rightOperand).getLiteral()).append("\n");
                return result.toString();
            }
        }

        // deal with value of right hand side of instruction first
        result.append(this.getCode(rhs, varTable, null));
        
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
                result.append(this.loadElement(method.getFirstArg(), varTable));
                result.append("\tarraylength\n");   // Does not update stack limits since it consumes the array reference and returns its length
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
        this.stackLimits.update(-1);

        return result.toString();
    }

    private String getCode(ReturnInstruction instruction, HashMap<String, Descriptor> varTable) {

        if (!instruction.hasReturnValue()) {
            return "\treturn\n";
        }

        StringBuilder result = new StringBuilder();

        ElementType type = instruction.getOperand().getType().getTypeOfElement();

        switch (type) {
            case VOID:
                result.append("\treturn\n");
                break;
            case INT32:
            case BOOLEAN:
                result.append(this.loadElement(instruction.getOperand(), varTable));
                result.append("\tireturn\n");
                this.stackLimits.update(-1);
                break;
            case ARRAYREF:
            case OBJECTREF:
                result.append(this.loadElement(instruction.getOperand(), varTable));
                result.append("\tareturn\n");
                this.stackLimits.update(-1);
                break;
            default:
                throw new RuntimeException("getCode: Unrecognized return instruction for " + type + " element type");
        }

        return result.toString();
    }

    private String getCode(GetFieldInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getFirstOperand(), varTable));
        result.append("\tgetfield ");   // Does not update stack limits since it consumes the obj reference and returns value
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
        this.stackLimits.update(-2);
        result.append(this.getElementClass(instruction.getFirstOperand())).append("/");
        result.append(((Operand) instruction.getSecondOperand()).getName()).append(" ");
        result.append(this.getJasminType(((Operand) instruction.getSecondOperand()).getType())).append("\n");

        return result.toString();
    }

    private String getCode(UnaryOpInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        result.append(this.loadElement(instruction.getOperand(), varTable));
        result.append("\ticonst_1\n");
        this.stackLimits.update(1);
        result.append("\tixor\n");
        this.stackLimits.update(-1);

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
                this.stackLimits.update(-1);
                break;
            case SUB:
                result.append("\tisub\n");
                this.stackLimits.update(-1);
                break;
            case MUL:
                result.append("\timul\n");
                this.stackLimits.update(-1);
                break;
            case DIV:
                result.append("\tidiv\n");
                this.stackLimits.update(-1);
                break;
            case LTH:
                result.append("\tif_icmplt ").append(this.getCondTrueLabel()).append("\n");
                this.stackLimits.update(-2);

                result.append("\ticonst_0\n");  // Does not update stack limits since we'll either push 0 or 1

                result.append("\tgoto ").append(this.getCondFalseLabel()).append("\n");
                result.append(this.getCondTrueLabel()).append(":\n");

                result.append("\ticonst_1\n");
                this.stackLimits.update(1);

                result.append(this.getCondFalseLabel()).append(":\n");
                this.numberCond++;
                break;
            case ANDB:
                result.append("\tiand\n");
                this.stackLimits.update(-1);
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
            this.stackLimits.update(-3);
            return "\tiastore\n";
        }

        switch (operand.getType().getTypeOfElement()) {
            case INT32:
            case BOOLEAN:
                this.stackLimits.update(-1);
                return "\tistore" + this.getVirtualReg(operand.getName(), varTable) + "\n";
            case OBJECTREF:
            case STRING:
            case ARRAYREF:
                this.stackLimits.update(-1);
                return "\tastore" + this.getVirtualReg(operand.getName(), varTable) + "\n";
            default:
                throw new RuntimeException("storeElement: Unrecognized operand type " + operand.getType());
        }
    }

    private String loadElement(Element element, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        if (element instanceof LiteralElement) {
            int n = Integer.parseInt(((LiteralElement) element).getLiteral());
            if (-128 <= n && n <= 127) {
                result.append("\tbipush ");
            } else {
                result.append("ldc ");
            }
            result.append(n).append("\n");
            this.stackLimits.update(1);
        } else if (element instanceof ArrayOperand) {
            ArrayOperand arrayOperand = (ArrayOperand) element;
            result.append("\taload").append(this.getVirtualReg(arrayOperand.getName(), varTable)).append("\n");
            this.stackLimits.update(1);
            
            result.append(this.loadElement(arrayOperand.getIndexOperands().get(0), varTable)); // TODO: Support for multiple dimensional arrays
            
            result.append("\tiaload\n");
            this.stackLimits.update(-1);
        } else if (element instanceof Operand) {
            Operand operand = (Operand) element;
            ElementType type = operand.getType().getTypeOfElement();
            switch (type) {
                case THIS:
                    result.append("\taload_0\n");
                    this.stackLimits.update(1);
                    break;
                case INT32:
                case BOOLEAN:
                    result.append("\tiload").append(this.getVirtualReg(operand.getName(), varTable)).append("\n");
                    this.stackLimits.update(1);
                    break;
                case OBJECTREF:
                case ARRAYREF:
                case STRING:
                    result.append("\taload").append(this.getVirtualReg(operand.getName(), varTable)).append("\n");
                    this.stackLimits.update(1);
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

        if (method.getInvocationType() != CallType.invokestatic) {
            this.stackLimits.update(-1);
        }
        this.stackLimits.update(- method.getListOfOperands().size());   // Update stack limits to consume objectref and arguments
        if (method.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.stackLimits.update(1);
        }

        return result.toString();
    }

    private String getNewCode(CallInstruction instruction, HashMap<String, Descriptor> varTable) {

        StringBuilder result = new StringBuilder();

        ElementType type = instruction.getFirstArg().getType().getTypeOfElement();

        switch (type) {
            case ARRAYREF:                
                result.append(this.loadElement(instruction.getListOfOperands().get(0), varTable));
                result.append("\tnewarray int\n");  // Does not update stack limits since it'll consume size and return array reference
                break;
            case OBJECTREF:
                result.append("\tnew ").append(((Operand) instruction.getFirstArg()).getName()).append("\n");
                this.stackLimits.update(1);
                break;
            default:
                throw new RuntimeException("getNewCode: Unrecognized new instruction for " + type + " element type");
        }

        return result.toString();
    }

    private String getVirtualReg(String name, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(name).getVirtualReg();
        return (virtualReg > 3 ? " " : "_") + virtualReg;
    }
}