package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.analysis.JmmType;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MethodTypeCheckVisitor extends AJmmVisitor<List<Report>, JmmType> {
    String methodSignature;
    SymbolTable symbolTable;
    Map<Symbol, Boolean> localVariables; // Boolean denotes if the variable has been initialized or not
    List<Symbol> parametersAndFields; // Field initialization check is not implemented because it depends on the method call order

    public MethodTypeCheckVisitor(SymbolTable symbolTable, String methodSignature) {
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;

        localVariables = new HashMap<>();
        for(var symbol : symbolTable.getLocalVariables(methodSignature)){
            localVariables.put(symbol, false);
        }
        parametersAndFields = new ArrayList<>();
        parametersAndFields.addAll(symbolTable.getParameters(methodSignature));
        parametersAndFields.addAll(symbolTable.getFields());

        addVisit("IntLiteral", this::visitIntLiteral);
        addVisit("Id", this::visitId);
        addVisit("LengthOp", this::visitLengthOp);
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("ClassMethod", this::visitClassMethod);
        addVisit("Condition", this::visitCondition);
        addVisit("Assignment", this::visitAssignment);
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayInitialization", this::visitArrayInitialization);
        addVisit("ClassInitialization", this::visitClassInitialization);
        addVisit("Bool", this::visitBool);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ExpressionInParentheses", this::visitExpressionInParentheses);
        addVisit("ArrayAssignment", this::visitArrayAssignment);
        addVisit("Argument", this::visitArgument);
        addVisit("ReturnExpression", this::visitReturnExpression);
        setDefaultVisit(this::defaultVisit);
    }

    private JmmType defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return new JmmType(null, false);
    }

    private JmmType visitIntLiteral(JmmNode node, List<Report> reports){
        return new JmmType("int", false);
    }

    private Symbol getSymbolByName(String name){
        for(Symbol s : this.localVariables.keySet()){
            if(name.equals(s.getName())){
                return s;
            }
        }
        for(Symbol s : this.parametersAndFields){
            if(name.equals(s.getName())){
                return s;
            }
        }
        return null;
    }

    private Symbol getSymbolByName(String name, JmmNode node, List<Report> reports){
        for(Symbol s : this.localVariables.keySet()){
            if(name.equals(s.getName())){
                if(this.localVariables.get(s) == false){ // Not initialized
                    reports.add(createSemanticError(node, "Variable " + name + " is not initialized"));
                }
                return s;
            }
        }
        for(Symbol s : this.parametersAndFields){
            if(name.equals(s.getName())){
                return s;
            }
        }
        return null;
    }
    private JmmType visitId(JmmNode node, List<Report> reports){
        String name = node.get("name");
        
        if(node.getJmmParent().getKind().equals("ClassMethod")){
            return new JmmType(null, false);
        }

        Symbol symbol = getSymbolByName(name, node, reports);
        if(symbol == null){
            reports.add(createSemanticError(node, "Symbol " + name + " is not defined." ));
            return new JmmType("", false);
        }
        return new JmmType(symbol.getType());
    }

    private JmmType visitLengthOp(JmmNode node, List<Report> reports){
        JmmNode child = node.getJmmChild(0);
        JmmType childType = visit(child, reports);
        if(!childType.isArray()){
            reports.add(createSemanticError(node, "Symbol doesn't support the .length op because it is not an array."));
            return new JmmType("", false);
        }
        return new JmmType("int", false);
    }

    private JmmType visitBinaryOp(JmmNode node, List<Report> reports){
        String op = node.get("op");
        JmmType firstChildType = visit(node.getJmmChild(0), reports);
        JmmType secondChildType = visit(node.getJmmChild(1), reports);

        JmmType boolType = new JmmType("boolean", false);
        JmmType intType = new JmmType("int",false);
        
        // AND,LOW,ADD,SUB,MUL,DIV
        switch(op){
            case "AND":
                if(firstChildType.equals(boolType) && secondChildType.equals(boolType)){
                    return boolType;
                } else {
                    reports.add(createSemanticError(node, "Invalid types for '&&' op"));
                }
                break;
            case "LOW":
                if(firstChildType.equals(intType) && secondChildType.equals(intType)){
                    return boolType;
                } else {
                    reports.add(createSemanticError(node, "Invalid types for '<' op"));
                }
                break;
            case "ADD":
            case "SUB":
            case "MUL":
            case "DIV": 
                if(firstChildType.equals(intType) && secondChildType.equals(intType)){
                    return intType;
                }
                else{ 
                    reports.add(createSemanticError(node, "Invalid type for " + op ));
                }
        }
        return new JmmType("", false);
    }

    private JmmType visitClassMethod(JmmNode node, List<Report> reports){
        String methodName = node.get("name");

        JmmNode classIdNode = node.getJmmChild(0);
        String className = classIdNode.get("name");
        Symbol symbol = getSymbolByName(className);

        JmmNode argumentsNode = node.getJmmChild(1);

        if(className.equals("this") || (symbol != null && symbol.getType().getName().equals(symbolTable.getClassName()))){
            if(symbolTable.getMethods().contains(methodName)){
                List<Symbol> methodParameters = symbolTable.getParameters(methodName);

                List<Report> methodCallReports = new ArrayList<>();
                System.out.println("VISIT --- " + methodName + " -> " + methodParameters.size() + " vs " + argumentsNode.getNumChildren());
                if(methodParameters.size() != argumentsNode.getNumChildren()){
                    methodCallReports.add(createSemanticError(node, "Invalid number of arguments for method " + methodName + " expected " + methodParameters.size() + " arguments but got " + argumentsNode.getNumChildren() + " instead"));
                } else {
                    for(int i = 0; i < methodParameters.size(); ++i){
                        Type parameterType = methodParameters.get(i).getType();
                        JmmType argumentType = visit(argumentsNode.getJmmChild(i), reports);
                        if(!argumentType.equals(parameterType)){
                            methodCallReports.add(createSemanticError(node, "Argument type doesn't match required parameter type for method " + methodName));
                        }
                    }
                }
                
                if(symbolTable.getSuper() == null){
                    reports.addAll(methodCallReports);    
                    return new JmmType(symbolTable.getReturnType(methodName));
                }
                return new JmmType(null, false, true);
            }
            if(symbolTable.getSuper() != null){
                return new JmmType(null, false, true);
            }
            reports.add(createSemanticError(node, "Method " + methodName + " does not exist."));
        } else {
            return new JmmType(null, false, true);
        }
    
        return new JmmType("", false);
    }

    private JmmType visitCondition(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("boolean", false))){
            reports.add(createSemanticError(node, "Condition is not a boolean"));
        }
        return new JmmType(null, false);
    }

    private JmmType visitAssignment(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        Symbol symbol = getSymbolByName(node.get("name"));
        if(!childType.equals(symbol.getType())){
            reports.add(createSemanticError(node, "Invalid assignment type for symbol " + symbol.getName()));
        }

        this.localVariables.computeIfPresent(symbol, (k, v) -> true);
        return new JmmType(null, false);
    }

    private JmmType visitArrayAssignment(JmmNode node, List<Report> reports){
        JmmType indexType = visit(node.getJmmChild(0), reports);
        if(!indexType.equals(new JmmType("int", false))){
            reports.add(createSemanticError(node, "Invalid type for array index"));
        }

        Symbol symbol = getSymbolByName(node.get("name"));
        Type symbolType = symbol.getType();
        JmmType assignType = visit(node.getJmmChild(1), reports);

        if(!symbolType.isArray()){
            reports.add(createSemanticError(node, "Symbol " + symbol.getName() + " is not an array"));
        }
        if(!assignType.equals(new JmmType(symbol.getType().getName(), false))){
            reports.add(createSemanticError(node, "Invalid type for array assignment"));
        }
        this.localVariables.computeIfPresent(symbol, (k, v) -> true);
        return new JmmType(null, false);
    }

    private JmmType visitArrayAccess(JmmNode node, List<Report> reports){
        String arrayName = node.getJmmChild(0).get("name");
        Symbol arraySymbol = getSymbolByName(arrayName);

        if(arraySymbol == null){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " not defined."));
        }

        JmmType type = new JmmType(arraySymbol.getType());

        if(!type.isArray()){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " is not an array."));
        }
        if(!(visit(node.getJmmChild(1), reports).equals(new JmmType("int", false)))){
            reports.add(createSemanticError(node, "Invalid array index"));
        }
        return new JmmType(type.getName(), false);
    }

    private JmmType visitArrayInitialization(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("int", false))){
            reports.add(createSemanticError(node, "Invalid type for array size"));
        }
        return new JmmType("int", true);
    }

    private JmmType visitClassInitialization(JmmNode node, List<Report> reports){
        return new JmmType(node.get("name"), false);
    }

    private JmmType visitBool(JmmNode node, List<Report> reports){
        return new JmmType("boolean", false);
    }

    private JmmType visitUnaryOp(JmmNode node, List<Report> reports){
        if(!node.get("op").equals("NEG")){ // Only NEG is supported by the jmm grammar
            return new JmmType(null, false);
        }
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(new JmmType("boolean",false))){
            reports.add(createSemanticError(node, "Invalid type for NEG op"));
        }
        return childType;
    }

    private JmmType visitExpressionInParentheses(JmmNode node, List<Report> reports){
        return visit(node.getJmmChild(0), reports);
    }

    private JmmType visitArgument(JmmNode node, List<Report> reports){
        return visit(node.getJmmChild(0), reports);
    }

    private JmmType visitReturnExpression(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0), reports);
        if(!childType.equals(symbolTable.getReturnType(this.methodSignature))){
            reports.add(createSemanticError(node, "Incompatible return type"));
        }
        return new JmmType(null, false);
    }

    private Report createSemanticError(JmmNode node, String message){
        return new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                message);
    }
}
