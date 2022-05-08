package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import pt.up.fe.comp.ollir.OllirStatement;

public class OllirStatementGenerator extends AJmmVisitor<Boolean, OllirStatement> {
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
    }

    private OllirStatement defaultVisit(JmmNode node, Boolean dummy){
        for(var child : node.getChildren()){
            visit(child);
        }
        return new OllirStatement("", "");
    }

    private OllirStatement visitAssignment(JmmNode node, Boolean dummy){
        if(node.getNumChildren() != 1){
            // TODO: Throw error?
        }

        StringBuilder code = new StringBuilder();
        String name = node.get("name");

        OllirStatement stmt = visit(node.getJmmChild(0));
        code.append(stmt.getCodeBefore());

        Symbol symbol = findLocal(name);
        if(symbol != null){

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
            code.append("  putfield(this, ").append(OllirUtils.getCode(symbol))
                .append(", ").append(stmt.getResultVariable()).append(").V;\n");
        }
        return new OllirStatement(code.toString(), OllirUtils.getCode(symbol));
    }

    private OllirStatement visitIntLiteral(JmmNode node, Boolean dummy){
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

    private OllirStatement visitBinaryOp(JmmNode node, Boolean dummy){
        StringBuilder code = new StringBuilder();

        String op = node.get("op");
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }

        OllirStatement stmt1 = visit(node.getJmmChild(0));
        OllirStatement stmt2 = visit(node.getJmmChild(1));
        code.append(stmt1.getCodeBefore());
        code.append(stmt2.getCodeBefore());

        String format = "";
        String temporary = "t" + temporaryVariableCounter++;
        switch(op){
            case "AND":
                temporary += ".bool";
                format = "  %s :=.bool %s &&.bool %s;\n"; 
                break;
            case "LOW":
                temporary += ".bool";
                format = "  %s :=.bool %s <.i32 %s;\n";
                break;
            case "ADD":
                temporary += ".i32"; 
                format = "  %s :=.i32 %s +.i32 %s;\n";
                break;
            case "SUB":
                temporary += ".i32";
                format = "  %s :=.i32 %s -.i32 %s;\n";
                break;
            case "MUL":
                temporary += ".i32";
                format = "  %s :=.i32 %s *.i32 %s;\n";
                break;
            case "DIV": 
                temporary += ".i32";
                format = "  %s :=.i32 %s /.i32 %s;\n";
                break;
        }
        code.append(String.format(format, temporary, stmt1.getResultVariable(), stmt2.getResultVariable()));
        return new OllirStatement(code.toString(), temporary);
    }

    private OllirStatement visitId(JmmNode node, Boolean dummy){
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
