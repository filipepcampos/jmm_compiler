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
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Id", this::visitId);
        addVisit("Bool", this::visitBool);
        addVisit("ReturnExpression", this::visitReturnExpression);
        addVisit("ClassMethod", this::visitClassMethod);
        addVisit("Arguments", this::visitArguments);
        addVisit("Argument", this::visitArgument);
        addVisit("StatementExpression", this::visitStatementExpression);
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

            code.append("  ")
                .append(OllirUtils.getCode(symbol))
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
            code.append("  putfield(this, ").append(OllirUtils.getCode(symbol))
                .append(", ").append(stmt.getResultVariable()).append(").V;\n");
        }
        
        return new OllirStatement(code.toString(), OllirUtils.getCode(symbol));
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

        String temporary = "t" + String.valueOf(temporaryVariableCounter++) + "." + returnType;
        String.format("  %s :=.%s %s %s.%s %s;\n", 
            temporary, returnType, stmt1.getResultVariable(), 
            opSymbol, operandType,stmt2.getResultVariable());

        return new OllirStatement(code.toString(), temporary);
    }

    private OllirStatement visitId(JmmNode node, String expectedType){
        String name = node.get("name");
        Symbol symbol = findLocal(name);
        if(symbol != null){
            return new OllirStatement("", OllirUtils.getCode(symbol));
        }
        symbol = findField(name);
        if(symbol != null){
            String type = OllirUtils.getCode(symbol.getType());
            String variable = "t" + temporaryVariableCounter++ + "." + type;
            String code = String.format("  %s :=.%s getfield(this, %s).%s;\n",
                variable, type, OllirUtils.getCode(symbol), type);
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
            .append("  ret.").append(type).append(" ")
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

        if(idName.equals("this")){
            code.append("  invokevirtual(this");
        }
        else if(idStmt.getResultVariable().isEmpty()){
            code.append("  invokestatic(").append(idName);
        } else {
            code.append(idStmt.getCodeBefore());
            code.append("  invokevirtual(").append(idStmt.getResultVariable());
        }
        code.append(", ").append("\"").append(methodName).append("\"");
        code.append(argumentStmt.getResultVariable()).append(")");

        // Return
        String returnTypeString = expectedType;
        if(idName.equals("this")) {
            Type returnType = symbolTable.getReturnType(methodName);
            returnTypeString = OllirUtils.getCode(returnType);
        }
        code.append(".").append(returnTypeString);

        if(returnTypeString.equals("V")){
            code.append(";\n");
            return new OllirStatement(code.toString(), "");   
        } else {
            StringBuilder temporaryAssignCode = new StringBuilder();
            String temporary = "t" + temporaryVariableCounter++ + "." + returnTypeString;
            temporaryAssignCode.append("  ").append(temporary)
                .append(" :=.").append(returnTypeString)
                .append(" ").append(code).append(";\n");
            return new OllirStatement(temporaryAssignCode.toString(), temporary);
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
        OllirStatement stmt = visit(node.getJmmChild(0), expectedType);
        return new OllirStatement(stmt.getCodeBefore() + "  " + stmt.getResultVariable() + ";\n", "");
    }

    private Symbol findField(String name){
        for(Symbol s:symbolTable.getFields()){
            if(s.getName().equals(name)){
                return s;
            }
        }
        return null;
    }

    private Symbol findLocal(String name){
        for(Symbol s : symbolTable.getLocalVariables(methodSignature)){
            if(s.getName().equals(name)){
                return s;
            }
        }
        for(Symbol s : symbolTable.getParameters(methodSignature)){
            if(s.getName().equals(name)){
                return s;
            }
        }
        return null;
    }
}
