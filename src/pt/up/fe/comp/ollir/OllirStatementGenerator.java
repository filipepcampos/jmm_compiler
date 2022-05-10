package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.ollir.OllirStatement;

import java.util.List;
import java.util.ArrayList;

public class OllirStatementGenerator extends AJmmVisitor<String, OllirStatement> {
    SymbolTable symbolTable;
    String methodSignature;
    Integer temporaryVariableCounter = 0;

    public OllirStatementGenerator(SymbolTable symbolTable, String methodSignature){
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;
        setDefaultVisit(this::defaultVisit);
        addVisit("Assignment", this::visitAssignment);
        addVisit("IntLiteral", this::visitIntLiteral);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Id", this::visitId);
        addVisit("Bool", this::visitBool);
        addVisit("ReturnExpression", this::visitReturnExpression);
        addVisit("ClassMethod", this::visitClassMethod);
        addVisit("Arguments", this::visitArguments);
        addVisit("Argument", this::visitArgument);
        addVisit("StatementExpression", this::visitStatementExpression);

        /*
        addVisit("LengthOp", this::visitLengthOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Condition", this::visitCondition);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayInitialization", this::visitArrayInitialization);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ExpressionInParentheses", this::visitExpressionInParentheses);
        addVisit("ArrayAssignment", this::visitArrayAssignment);
        */
    }

    private OllirStatement defaultVisit(JmmNode node, String expectedType){
        for(var child : node.getChildren()){
            visit(child, "");
        }
        return new OllirStatement("", "");
    }

    private OllirStatement visitAssignment(JmmNode node, String dummy){
        if(node.getNumChildren() != 1){
            // TODO: Throw error?
        }
        StringBuilder code = new StringBuilder();
        String name = node.get("name");

        Symbol symbol = findLocal(name);
        if(symbol != null){
            OllirStatement stmt = visit(node.getJmmChild(0), OllirUtils.getCode(symbol.getType()));
            code.append(stmt.getCodeBefore());

            code.append(symbol.getName())
                .append(" :=.").append(OllirUtils.getCode(symbol.getType()))
                .append(" ");

            code.append(stmt.getResultVariable()).append(";\n");
        } else {
            symbol = findField(name);
            if(symbol == null){
                // TODO: Error
            }
            OllirStatement stmt = visit(node.getJmmChild(0), OllirUtils.getCode(symbol.getType()));
            code.append(stmt.getCodeBefore());
            code.append("putfield(this, ").append(symbol.getName())
                .append(", ").append(stmt.getResultVariable()).append(").V;\n");
        }
        
        return new OllirStatement(code.toString(), symbol.getName());
    }

    private OllirStatement visitIntLiteral(JmmNode node, String expectedType){
        String type = node.get("type");
        String stringValue = node.get("value");
        int value = 0;
        switch(type){
            case "decimal": value = Integer.parseInt(stringValue);
            break;
            case "binary": value = Integer.parseInt(stringValue, 2);
            break;
            case "octal": value = Integer.parseInt(stringValue, 8);
            break;
            case "hexadecimal": value = Integer.parseInt(stringValue, 16);
            break;
        }
        return new OllirStatement("", String.valueOf(value) + ".i32");
    }

    private OllirStatement visitBool(JmmNode node, String expectedType){
        String stringValue = node.get("value");
        Integer value = stringValue.equals("true") ? 1 : 0;
        return new OllirStatement("", String.valueOf(value) + ".bool");
    }

    private OllirStatement visitBinaryOp(JmmNode node, String expectedType){
        StringBuilder code = new StringBuilder();

        String op = node.get("op");
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }

        String returnType = "";
        String operandType = "";
        String opSymbol = "";
        switch(op){
            case "AND":
                opSymbol = "&&"; returnType = "bool"; operandType = "bool";
                break;
            case "LOW":
                opSymbol = "<"; returnType = "bool"; operandType = "i32";
                break;
            case "ADD":
                opSymbol = "+"; returnType = "i32"; operandType = "i32";
                break;
            case "SUB":
                opSymbol = "-"; returnType = "i32"; operandType = "i32";
                break;
            case "MUL":
                opSymbol = "*"; returnType = "i32"; operandType = "i32";
                break;
            case "DIV": 
                opSymbol = "/"; returnType = "i32"; operandType = "i32";
                break;
        }

        OllirStatement stmt1 = visit(node.getJmmChild(0), operandType);
        OllirStatement stmt2 = visit(node.getJmmChild(1), operandType);
        code.append(stmt1.getCodeBefore());
        code.append(stmt2.getCodeBefore());

        String rhs = String.format("%s %s.%s %s", stmt1.getResultVariable(), opSymbol, operandType,stmt2.getResultVariable());
        String temporaryVariable = assignTemporary(returnType, rhs, code);

        return new OllirStatement(code.toString(), temporaryVariable);
    }

    private OllirStatement visitUnaryOp(JmmNode node, String expectedType){
        StringBuilder code = new StringBuilder();

        String op = node.get("op");
        if(node.getNumChildren() != 1){
            // TODO: Error?
        }
        if(!op.equals("NEG")){
            // TODO: Throw error
        }
        return new OllirStatement(code.toString(), "");
    }

    private OllirStatement visitId(JmmNode node, String expectedType){
        String name = node.get("name");
        Symbol symbol = findLocal(name);
        if(symbol != null){
            return new OllirStatement("", symbol.getName());
        }
        symbol = findField(name);
        if(symbol != null){
            String type = OllirUtils.getCode(symbol.getType());
            String variable = "t" + temporaryVariableCounter++ + "." + type;
            String code = String.format("%s :=.%s getfield(this, %s).%s;\n",
                variable, type, symbol.getName(), type);
            return new OllirStatement(code, variable);
        }
        return new OllirStatement("", "");
    }

    private OllirStatement visitReturnExpression(JmmNode node, String expectedType){
        StringBuilder code = new StringBuilder();
        if(node.getNumChildren() != 1){
            // TODO: Throw error
        }
        OllirStatement stmt = visit(node.getJmmChild(0), expectedType);

        String[] splitString = stmt.getResultVariable().split("\\.");
        String type = splitString[splitString.length-1];
        code.append(stmt.getCodeBefore())
            .append("ret.").append(type).append(" ")
            .append(stmt.getResultVariable()).append(";\n");
        return new OllirStatement(code.toString(), "");
    }

    private OllirStatement visitClassMethod(JmmNode node, String expectedType){
        if(node.getNumChildren() != 2){
            // TODO: Throw error
        }
        StringBuilder code = new StringBuilder();
        String methodName = node.get("name");

        // ID Node
        JmmNode idNode = node.getJmmChild(0);
        OllirStatement idStmt = visit(idNode, "");
        String idName = idNode.get("name");

        // Arguments Node
        OllirStatement argumentStmt = visit(node.getJmmChild(1), methodName);
        code.append(argumentStmt.getCodeBefore());

        StringBuilder methodCallCode = new StringBuilder();

        // Choose correct invoke
        if(idName.equals("this")){
            methodCallCode.append("invokevirtual(this");
        }
        else if(idStmt.getResultVariable().isEmpty()){
            methodCallCode.append("invokestatic(").append(idName);
        } else {
            methodCallCode.append(idStmt.getCodeBefore());
            methodCallCode.append("invokevirtual(").append(idStmt.getResultVariable());
        }
        methodCallCode.append(", ").append("\"").append(methodName).append("\"");
        methodCallCode.append(argumentStmt.getResultVariable()).append(")");

        // Return
        String returnTypeString = expectedType;
        System.out.println(methodName + "--> " + expectedType);
        if(idName.equals("this")) {
            Type returnType = symbolTable.getReturnType(methodName);
            returnTypeString = OllirUtils.getCode(returnType);
        }
        methodCallCode.append(".").append(returnTypeString);

        if(returnTypeString.equals("V")){
            code.append(methodCallCode).append(";\n");
            return new OllirStatement(code.toString(), "");   
        } else {
            String temporaryVariable = assignTemporary(returnTypeString, methodCallCode.toString(), code);
            return new OllirStatement(code.toString(), temporaryVariable);
        }
    }

    // Receives methodName to search for parameter types
    private OllirStatement visitArguments(JmmNode node, String methodName){
        StringBuilder code = new StringBuilder();
        StringBuilder argumentList = new StringBuilder();

        Boolean parametersAreAvailable = false;
        List<Symbol> parameters;
        if(symbolTable.getMethods().contains(methodName)){
            parameters = symbolTable.getParameters(methodName);
            parametersAreAvailable = true;
        } else {
            parameters = new ArrayList<>();
        }

        for(int i = 0; i < node.getNumChildren(); ++i){
            OllirStatement childStmt;
            if(parametersAreAvailable){
                childStmt = visit(node.getJmmChild(i), OllirUtils.getCode(parameters.get(i).getType()));
            } else {
                childStmt = visit(node.getJmmChild(i), "");
            }
            code.append(childStmt.getCodeBefore());
            argumentList.append(", ").append(childStmt.getResultVariable());
        }
        return new OllirStatement(code.toString(), argumentList.toString());
    }

    private OllirStatement visitArgument(JmmNode node, String expectedType){
        if(node.getNumChildren() != 1){
            // TODO: Throw error
        }
        return visit(node.getJmmChild(0), expectedType);
    }

    private OllirStatement visitStatementExpression(JmmNode node, String expectedType){
        if(node.getNumChildren() != 1){
            // TODO: Throw error
        }
        OllirStatement stmt = visit(node.getJmmChild(0), "V");
        return new OllirStatement(stmt.getCodeBefore(), "");
    }

    // Appends a new temporary assignment to the code StringBuilder and returns the variable name
    private String assignTemporary(String type, String rhs, StringBuilder code){
        String temporary = "t" + temporaryVariableCounter++ + "." + type;
        code.append(temporary).append(" :=.").append(type).append(" ").append(rhs).append(";\n");
        return temporary;
    }

    // Return symbol for a given field
    // The symbol name is already an valid OLLIR code
    private Symbol findField(String name){
        for(Symbol s:symbolTable.getFields()){
            if(s.getName().equals(name)){
                return new Symbol(s.getType(), OllirUtils.getCode(s));
            }
        }
        return null;
    }

    // Find local variable or parameter and return corresponding symbol
    // The symbol name is already an valid OLLIR code
    private Symbol findLocal(String name){
        for(Symbol s : symbolTable.getLocalVariables(methodSignature)){
            if(s.getName().equals(name)){
                return new Symbol(s.getType(), OllirUtils.getCode(s));
            }
        }

        List<Symbol> parameters = symbolTable.getParameters(methodSignature);
        for(int i = 0; i < parameters.size(); ++i){
            Symbol s = parameters.get(i);
            if(s.getName().equals(name)){
                if(methodSignature.equals("main")) { // Static
                    return new Symbol(s.getType(), "$" + String.valueOf(i) + "." + OllirUtils.getCode(s));
                } else {
                    return new Symbol(s.getType(), "$" + String.valueOf(i+1) + "." + OllirUtils.getCode(s));
                }
            }
        }
        return null;
    }
}
