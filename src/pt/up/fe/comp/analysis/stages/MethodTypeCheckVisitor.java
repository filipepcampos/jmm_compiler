package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
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

public class MethodTypeCheckVisitor extends PreorderJmmVisitor<List<Report>, JmmType> {
    String methodSignature;
    SymbolTable symbolTable;
    List<Symbol> variables;

    public MethodTypeCheckVisitor(SymbolTable symbolTable, String methodSignature) {
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;
        
        variables = symbolTable.getLocalVariables(methodSignature);
        variables.addAll(symbolTable.getParameters(methodSignature));
        variables.addAll(symbolTable.getFields());

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
        /*
        VarDeclaration / Arguments / Parameter - não necessário
        */
        // TODO: ReturnExpression?
    }

    private JmmType visitIntLiteral(JmmNode node, List<Report> reports){
        return new JmmType("int", false);
    }

    private Symbol getSymbolByName(String name){
        for(Symbol s : variables){
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

        Symbol symbol = getSymbolByName(name);
        if(symbol == null){
            reports.add(createSemanticError(node, "Symbol " + name + " is not defined." ));
            return new JmmType(null, false);
        }
        return new JmmType(symbol.getType());
    }

    private JmmType visitLengthOp(JmmNode node, List<Report> reports){
        JmmType childType = visit(node.getJmmChild(0));
        if(!childType.isArray()){
            reports.add(createSemanticError(node, "Symbol is not an array."));
            return new JmmType(null, false);
        }
        return new JmmType("int", false);
    }

    private JmmType visitBinaryOp(JmmNode node, List<Report> reports){
        String op = node.get("op");
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }
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
        return new JmmType(null, false);
    }

    private JmmType visitClassMethod(JmmNode node, List<Report> reports){
        String methodName = node.get("name");

        if(node.getNumChildren() != 2){
            // TODO: Error?
        }


        JmmNode classIdNode = node.getJmmChild(0);
        String className = classIdNode.get("name");
        // TODO Verify if className == 'this' or if it exists in symbolTable

        JmmNode argumentsNode = node.getJmmChild(1);
        // TODO: Visit arguments

        if(className.equals("this")){
            if(symbolTable.getMethods().contains(methodName)){
                List<Symbol> methodParameters = symbolTable.getParameters(methodName);

                List<Report> methodCallReports = new ArrayList<>();
                if(methodParameters.size() != argumentsNode.getNumChildren()){
                    methodCallReports.add(createSemanticError(node, "Invalid number of arguments"));
                }
                
                for(int i = 0; i < methodParameters.size(); ++i){
                    Type parameterType = methodParameters.get(i).getType();
                    JmmType argumentType = visit(argumentsNode.getJmmChild(i));
                    if(!argumentType.equals(parameterType)){
                        methodCallReports.add(createSemanticError(node, "Argument type doesn't match required parameter type"));
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

            // TODO: Check if class is imported?
            return new JmmType(null, false, true);
        }
        // TODO: Method doesnt exist

    
        return new JmmType(null, false);
    }

    private JmmType visitCondition(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Error?
        }
        JmmType childType = visit(node.getJmmChild(0));
        if(!childType.equals(new JmmType("boolean", false))){
            reports.add(createSemanticError(node, "Condition child is not a boolean"));
        }
        return new JmmType(null, false);
    }

    private JmmType visitAssignment(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Error?
        }
        JmmType childType = visit(node.getJmmChild(0));
        Symbol symbol = getSymbolByName(node.get("name"));
        System.out.println(node.get("name") + " - " + symbol + " -- " + childType + "((" + node.get("line") + "))");
        if(!childType.equals(symbol.getType())){
            System.out.println("debug -> " + node.get("name") + " " + symbol + "  = " + childType);
            reports.add(createSemanticError(node, "Invalid assignment type"));
        }
        return new JmmType(null, false);
    }

    private JmmType visitArrayAssignment(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }

        JmmType indexType = visit(node.getJmmChild(0));
        if(!indexType.equals(new JmmType("int", false))){
            reports.add(createSemanticError(node, "Invalid type for array index"));
        }

        Symbol symbol = getSymbolByName(node.get("name"));
        Type symbolType = symbol.getType();
        JmmType assignType = visit(node.getJmmChild(1));

        if(!symbolType.isArray()){
            reports.add(createSemanticError(node, "Symbol is not an array"));
        }
        if(!assignType.equals(new JmmType(symbol.getType().getName(), false))){
            reports.add(createSemanticError(node, "Invalid type for array assignment"));
        }

        return new JmmType(null, false);
    }

    private JmmType visitArrayAccess(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }
        
        String arrayName = node.getJmmChild(0).get("name");
        Symbol arraySymbol = getSymbolByName(arrayName);

        if(arraySymbol == null){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " not defined."));
        }

        JmmType type = new JmmType(arraySymbol.getType());

        if(!type.isArray()){
            reports.add(createSemanticError(node, "Symbol " + arrayName + " is not an array."));
        }
        if(!(visit(node.getJmmChild(1)).equals(new JmmType("int", false)))){
            reports.add(createSemanticError(node, "Invalid array index"));
        }
        return new JmmType(type.getName(), false); // TODO: Are arrays inside arrays allowed?
    }

    private JmmType visitArrayInitialization(JmmNode node, List<Report> reports){
        return new JmmType("int", true);
    }

    private JmmType visitClassInitialization(JmmNode node, List<Report> reports){
        return new JmmType(node.get("name"), false);
    }

    private JmmType visitBool(JmmNode node, List<Report> reports){
        return new JmmType("boolean", false);
    }

    private JmmType visitUnaryOp(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Throw error?
        }
        if(!node.get("op").equals("NEG")){
            // TODO: Throw error
        }
        JmmType childType = visit(node.getJmmChild(0));
        System.out.println("UNARY_OP: " + node.getJmmChild(0) + "  " + childType);
        if(!childType.equals(new JmmType("boolean",false))){
            System.out.println("NOT A BOOLEAN");
            reports.add(createSemanticError(node, "Invalid type for NEG op"));
        }
        return childType;
    }

    private JmmType visitExpressionInParentheses(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Throw error?
        }
        return visit(node.getJmmChild(0));
    }

    private JmmType visitArgument(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Throw error?
        }
        return visit(node.getJmmChild(0));
    }

    private Report createSemanticError(JmmNode node, String message){
        return new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                message);
    }
}
