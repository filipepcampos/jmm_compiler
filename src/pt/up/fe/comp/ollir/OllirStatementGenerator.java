package pt.up.fe.comp.ollir;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import java.util.List;
import java.util.ArrayList;

public class OllirStatementGenerator extends AJmmVisitor<OllirGeneratorHint, OllirStatement> {
    SymbolTable symbolTable;
    String methodSignature;
    Integer temporaryVariableCounter = 0;
    Integer labelCounter = 0;

    public OllirStatementGenerator(SymbolTable symbolTable, String methodSignature){
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;
        setDefaultVisit(this::defaultVisit);
        addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
        addVisit(AstNode.INT_LITERAL, this::visitIntLiteral);
        addVisit(AstNode.UNARY_OP, this::visitUnaryOp);
        addVisit(AstNode.BINARY_OP, this::visitBinaryOp);
        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.BOOL, this::visitBool);
        addVisit(AstNode.RETURN_EXPRESSION, this::visitReturnExpression);
        addVisit(AstNode.CLASS_METHOD, this::visitClassMethod);
        addVisit(AstNode.ARGUMENTS, this::visitArguments);
        addVisit(AstNode.ARGUMENT, this::visitArgument);
        addVisit(AstNode.STATEMENT_EXPRESSION, this::visitStatementExpression);
        addVisit(AstNode.CLASS_INITIALIZATION, this::visitClassInitialization);
        addVisit(AstNode.EXPRESSION_IN_PARENTHESES, this::visitExpressionInParentheses);
        addVisit(AstNode.IF_STATEMENT, this::visitIfStatement);
        addVisit(AstNode.CONDITION, this::visitCondition);
        addVisit(AstNode.STATEMENT_SCOPE, this::visitStatementScope);

        /*
        LENGTH_OP,
        CONDITION,
        ARRAY_ACCESS,
        ARRAY_INITIALIZATION,
        ARRAY_ASSIGNMENT
        WHILE_STATEMENT,
        STATEMENT_SCOPE
        */
    }

    private OllirStatement defaultVisit(JmmNode node, OllirGeneratorHint hint){
        for(var child : node.getChildren()){
            visit(child, hint);
        }
        return new OllirStatement("", "");
    }

    private OllirStatement visitAssignment(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();
        String name = node.get("name");

        Symbol symbol = findLocal(name);
        if(symbol != null){
            OllirGeneratorHint hintForChild = new OllirGeneratorHint(OllirUtils.getCode(symbol.getType()), hint.getMethodSignature(), false);
            OllirStatement stmt = visit(node.getJmmChild(0), hintForChild);
            code.append(stmt.getCodeBefore());

            code.append(symbol.getName())
                .append(" :=.").append(OllirUtils.getCode(symbol.getType()))
                .append(" ");

            code.append(stmt.getResultVariable()).append(";\n");
        } else {
            symbol = findField(name);
            OllirGeneratorHint hintForChild = new OllirGeneratorHint(OllirUtils.getCode(symbol.getType()), hint.getMethodSignature(), false);
            OllirStatement stmt = visit(node.getJmmChild(0), hintForChild);
            code.append(stmt.getCodeBefore());
            code.append("putfield(this, ").append(symbol.getName())
                .append(", ").append(stmt.getResultVariable()).append(").V;\n");
        }
        
        return new OllirStatement(code.toString(), symbol.getName());
    }

    private OllirStatement visitIntLiteral(JmmNode node, OllirGeneratorHint hint){
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

    private OllirStatement visitBool(JmmNode node, OllirGeneratorHint hint){
        String stringValue = node.get("value");
        Integer value = stringValue.equals("true") ? 1 : 0;
        return new OllirStatement("", String.valueOf(value) + ".bool");
    }

    private OllirStatement visitBinaryOp(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();

        String op = node.get("op");

        String returnType = "";
        String operandType = "";
        String opSymbol = "";
        switch(op){
            case "AND":
                opSymbol = "&&.bool"; returnType = "bool"; operandType = "bool";
                break;
            case "LOW":
                opSymbol = "<.bool"; returnType = "bool"; operandType = "i32";
                break;
            case "ADD":
                opSymbol = "+.i32"; returnType = "i32"; operandType = "i32";
                break;
            case "SUB":
                opSymbol = "-.i32"; returnType = "i32"; operandType = "i32";
                break;
            case "MUL":
                opSymbol = "*.i32"; returnType = "i32"; operandType = "i32";
                break;
            case "DIV": 
                opSymbol = "/.i32"; returnType = "i32"; operandType = "i32";
                break;
        }

        OllirGeneratorHint hintForChild = new OllirGeneratorHint(hint.getMethodSignature(), operandType, true);
        OllirStatement stmt1 = visit(node.getJmmChild(0), hintForChild);
        OllirStatement stmt2 = visit(node.getJmmChild(1), hintForChild);
        code.append(stmt1.getCodeBefore());
        code.append(stmt2.getCodeBefore());

        String rhs = String.format("%s %s %s", stmt1.getResultVariable(), opSymbol, stmt2.getResultVariable());
        if(hint.needsTemporaryVar()) {
            String temporaryVariable = assignTemporary(returnType, rhs, code);
            return new OllirStatement(code.toString(), temporaryVariable);
        }
        return new OllirStatement(code.toString(), rhs);
    }

    private OllirStatement visitUnaryOp(JmmNode node, OllirGeneratorHint hint){ // TODO: Implement
        StringBuilder code = new StringBuilder();

        String op = node.get("op");
        if(!op.equals("NEG")){
        }
        return new OllirStatement(code.toString(), "");
    }

    private OllirStatement visitId(JmmNode node, OllirGeneratorHint hint){
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

    private OllirStatement visitReturnExpression(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();
        OllirStatement stmt = visit(node.getJmmChild(0),
                new OllirGeneratorHint(hint.getMethodSignature(), hint.getExpectedType(), true));

        String[] splitString = stmt.getResultVariable().split("\\.");
        String type = splitString[splitString.length-1];
        code.append(stmt.getCodeBefore())
            .append("ret.").append(type).append(" ")
            .append(stmt.getResultVariable()).append(";\n");
        return new OllirStatement(code.toString(), "");
    }

    private OllirStatement visitClassMethod(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();
        String methodName = node.get("name");

        // ID Node
        JmmNode idNode = node.getJmmChild(0);
        OllirStatement idStmt = visit(idNode, hint);
        String idName = idNode.get("name");

        // Arguments Node
        OllirStatement argumentStmt = visit(node.getJmmChild(1), new OllirGeneratorHint(idName));
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
        String returnTypeString = hint.getExpectedType();
        boolean returnTypeIsKnown = false;
        if(idName.equals("this") || idName.equals(symbolTable.getClassName()) || idName.equals(symbolTable.getSuper())) {
            returnTypeIsKnown = true;
        }
        else {
            Symbol s = findSymbol(idName);
            if(s != null){
                String symbolTypeName = s.getType().getName();
                if(!s.getType().isArray()
                    && (symbolTypeName.equals(symbolTable.getClassName()) || symbolTypeName.equals(symbolTable.getSuper())
                )){
                    returnTypeIsKnown = true;
                }
            }
        }

        if(returnTypeIsKnown){
            Type returnType = symbolTable.getReturnType(methodName);
            returnTypeString = OllirUtils.getCode(returnType);
        }

        methodCallCode.append(".").append(returnTypeString);

        if(returnTypeString.equals("V")){
            code.append(methodCallCode).append(";\n");
            return new OllirStatement(code.toString(), "");   
        } else {
            if(hint.needsTemporaryVar()){
                String temporaryVariable = assignTemporary(returnTypeString, methodCallCode.toString(), code);
                return new OllirStatement(code.toString(), temporaryVariable);
            } else {
                return new OllirStatement(code.toString(), methodCallCode.toString());
            }
        }
    }

    // Receives methodName to search for parameter types
    private OllirStatement visitArguments(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();
        StringBuilder argumentList = new StringBuilder();

        boolean parametersAreAvailable = false;
        List<Symbol> parameters;
        if(symbolTable.getMethods().contains(hint.getMethodSignature())){
            parameters = symbolTable.getParameters(hint.getMethodSignature());
            parametersAreAvailable = true;
        } else {
            parameters = new ArrayList<>();
        }

        for(int i = 0; i < node.getNumChildren(); ++i){
            OllirStatement childStmt;
            if(parametersAreAvailable){
                OllirGeneratorHint childHint = new OllirGeneratorHint(hint.getMethodSignature(),
                        OllirUtils.getCode(parameters.get(i).getType()), true);
                childStmt = visit(node.getJmmChild(i), childHint);
            } else {
                childStmt = visit(node.getJmmChild(i), new OllirGeneratorHint(hint.getMethodSignature()));
            }
            code.append(childStmt.getCodeBefore());
            argumentList.append(", ").append(childStmt.getResultVariable());
        }
        return new OllirStatement(code.toString(), argumentList.toString());
    }

    private OllirStatement visitArgument(JmmNode node, OllirGeneratorHint hint){
        return visit(node.getJmmChild(0), hint);
    }

    private OllirStatement visitStatementExpression(JmmNode node, OllirGeneratorHint hint){
        OllirStatement stmt = visit(node.getJmmChild(0), new OllirGeneratorHint(hint.getMethodSignature(), "V", true));
        return new OllirStatement(stmt.getCodeBefore(), "");
    }

    private OllirStatement visitClassInitialization(JmmNode node, OllirGeneratorHint hint) {
        StringBuilder code = new StringBuilder();
        String className = node.get("name");
        String temporaryVar = assignTemporary(className, String.format("new(%s).%s", className, className), code);
        code.append("invokespecial(").append(temporaryVar).append(", \"<init>\").V;\n");
        return new OllirStatement(code.toString(), temporaryVar);
    }

    private OllirStatement visitExpressionInParentheses(JmmNode node, OllirGeneratorHint hint) {
        JmmNode child = node.getJmmChild(0);
        return visit(child, hint);
    }

    private OllirStatement visitIfStatement(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();

        JmmNode conditionNode = node.getJmmChild(0);
        JmmNode ifNode = node.getJmmChild(1);
        JmmNode elseNode = node.getJmmChild(2);

        OllirStatement conditionStatement = visit(conditionNode, hint);
        OllirStatement ifStatement = visit(ifNode, hint);
        OllirStatement elseStatement = visit(elseNode, hint);

        code.append(conditionStatement.getCodeBefore())
            .append(String.format("if(%s) goto else%d;\n", conditionStatement.getResultVariable(), labelCounter));
        code.append(ifStatement.getCodeBefore()).append("goto endif").append(labelCounter).append(";\n");
        code.append(String.format("else%d:\n", labelCounter));
        code.append(elseStatement.getCodeBefore()).append("\nendif").append(labelCounter).append(":\n");

        return new OllirStatement(code.toString(),"" );
    }

    private OllirStatement visitCondition(JmmNode node, OllirGeneratorHint hint){
        return visit(node.getJmmChild(0), new OllirGeneratorHint(methodSignature, "bool", false));
    }

    private OllirStatement visitStatementScope(JmmNode node, OllirGeneratorHint hint){
        StringBuilder code = new StringBuilder();
        for(JmmNode child : node.getChildren()){
            OllirStatement stmt = visit(child, hint);
            code.append(stmt.getCodeBefore());
        }
        return new OllirStatement(code.toString(), "");
    }
 
    // Appends a new temporary assignment to the code StringBuilder and returns the variable name
    private String assignTemporary(String type, String rhs, StringBuilder code){
        String temporary = "t" + temporaryVariableCounter++ + "." + type;
        code.append(temporary).append(" :=.").append(type).append(" ").append(rhs).append(";\n");
        return temporary;
    }

    private Symbol findSymbol(String name){
        Symbol s = findLocal(name);
        if(s == null){
            return findField(name);
        }
        return s;
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
