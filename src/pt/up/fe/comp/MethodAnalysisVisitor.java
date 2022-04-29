package pt.up.fe.comp;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.List;
import java.util.ArrayList;

public class MethodAnalysisVisitor extends PreorderJmmVisitor<List<Report>, Type> {
    String methodSignature;
    SymbolTable symbolTable;
    List<Symbol> variables;

    public MethodAnalysisVisitor(SymbolTable symbolTable, String methodSignature) {
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
    }

    public Type visitIntLiteral(JmmNode node, List<Report> reports){
        return new Type("int", false);
    }

    public Symbol getSymbolByName(String name){
        for(Symbol s : variables){
            if(name.equals(s.getName())){
                return s;
            }
        }
        return null;
    }

    public Type visitId(JmmNode node, List<Report> reports){
        String name = node.get("name");
        
        if(node.getJmmParent().getKind().equals("ClassMethod")){
            return new Type(null, false);
        }

        Symbol symbol = getSymbolByName(name);
        if(symbol == null){
            // TODO: ask how to get Line 
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Symbol " + name + " not defined."));
            return new Type(null, false);
        }
        return symbol.getType();
    }

    public Type visitLengthOp(JmmNode node, List<Report> reports){
        Type childType = visit(node.getJmmChild(0));
        if(!childType.isArray()){
            // TODO: ask how to get Line 
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Symbol is not an array."));
            return new Type(null, false);
        }
        return new Type("int", false);
    }

    public Type visitBinaryOp(JmmNode node, List<Report> reports){
        String op = node.get("op");
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }
        Type firstChildType = visit(node.getJmmChild(0), reports);
        Type secondChildType = visit(node.getJmmChild(1), reports);

        Type boolType = new Type("boolean", false);
        Type intType = new Type("int",false);

        // AND,LOW,ADD,SUB,MUL,DIV
        switch(op){
            case "AND":
                if(firstChildType.equals(boolType) && secondChildType.equals(boolType)){
                    return boolType;
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid types for '&&' op"));
                }
                break;
            case "LOW":
                if(firstChildType.equals(intType) && secondChildType.equals(intType)){
                    return boolType;
                } else {
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid types for '<' op"));
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
                    reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid type for " + op ));
                }
        }
        return new Type(null, false);
    }

    public Type visitClassMethod(JmmNode node, List<Report> reports){
        String methodName = node.get("name");

        if(node.getNumChildren() != 2){
            // TODO: Error?
        }


        JmmNode classIdNode = node.getJmmChild(0);
        String className = classIdNode.get("name");
        // TODO Verify if className == 'this' or if it exists in symbolTable

        JmmNode argumentsNode = node.getJmmChild(1);
        // TODO: Visit arguments



        if(className == "this"){
            for(String mName : symbolTable.getMethods()){
                if(methodName.equals(mName)){
                    return symbolTable.getReturnType(methodName);
                }
            }
            if(symbolTable.getSuper() != null){
                return new Type(null, false);
            }
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Method does not exist"));
        } else {
            // TODO 
        }
        // TODO: Method doesnt exist

    
        return new Type(null, false);
    }

    public Type visitCondition(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Error?
        }
        Type childType = visit(node.getJmmChild(0));
        if(!childType.equals(new Type("boolean", false))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Condition child is not a boolean"));
        }
        return new Type(null, false);
    }

    public Type visitAssignment(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 1){
            // TODO: Error?
        }
        Type childType = visit(node.getJmmChild(0));
        Symbol symbol = getSymbolByName(node.get("name"));
        System.out.println(node.get("name") + " - " + symbol + " -- " + childType);
        if(!childType.equals(symbol.getType())){
            System.out.println("debug -> " + node.get("name") + " " + symbol + "  = " + childType);
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid assignment type"));
        }
        return new Type(null, false);
    }

    public Type visitArrayAccess(JmmNode node, List<Report> reports){
        if(node.getNumChildren() != 2){
            // TODO: Error?
        }
        
        String arrayName = node.getJmmChild(0).get("name");
        Symbol arraySymbol = getSymbolByName(arrayName);

        if(arraySymbol == null){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Symbol " + arrayName + " not defined."));
        }

        Type type = arraySymbol.getType();

        if(!type.isArray()){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Symbol " + arrayName + " is not an array."));
        }
        if(!(visit(node.getJmmChild(1)).equals(new Type("int", false)))){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC, -1, "Invalid array index"));
        }
        return new Type(type.getName(), false); // TODO: Are arrays inside arrays allowed?
    }

    public Type visitArrayInitialization(JmmNode node, List<Report> reports){
        return new Type("int", true);
    }

    public Type visitClassInitialization(JmmNode node, List<Report> reports){
        return new Type(node.get("name"), false);
    }
}

/* 
Program
   ImportDecl (name: MathUtils)
   ImportDecl (name: Quicksort)
   ImportDecl (name: Something)
      SubPackage (name: other)
      SubPackage (name: othe)
      SubPackage (name: oth)
   ClassDeclaration (className: Lazysort, baseClassNam e: Quicksort)
      MainMethodDeclaration
         Parameter (name: a, type: String[])
         VarDeclaration (name: L, type: int[])
         VarDeclaration (name: i, type: int)
         VarDeclaration (name: d, type: boolean)
         VarDeclaration (name: q, type: Lazysort)
         StatementScope
            Assignment (name: i)
               BinaryOp (op: ADD)
                  IntLiteral (type: decimal, value: 1)
                  BinaryOp (op: MUL)
                     IntLiteral (type: decimal, value: 2)
                     IntLiteral (type: decimal, value: 3)
            Assignment (name: i)
               BinaryOp (op: MUL)
                  Id (name: i)
                  Id (name: i)
         Assignment (name: L)
            ArrayInitialization
               IntLiteral (type: decimal, value: 10)
         Assignment (name: i)
            BinaryOp (op: ADD)
               BinaryOp (op: ADD)
                  BinaryOp (op: ADD)
                     BinaryOp (op: ADD)
                        IntLiteral (type: decimal, value: 1)
                        IntLiteral (type: decimal, value: 2)
                     IntLiteral (type: decimal, value: 3)
                  BinaryOp (op: MUL)
                     IntLiteral (type: decimal, value: 4)
                     IntLiteral (type: decimal, value: 5)
               IntLiteral (type: decimal, value: 6)
         Assignment (name: i)
            IntLiteral (type: decimal, value: 0)
         WhileStatement
            Condition
               BinaryOp (op: LOW)
                  Id (name: i)
                  LengthOp
                     Id (name: L)
            StatementScope
               ArrayAssignment (name: L)
                  Id (name: i)
                  BinaryOp (op: SUB)
                     LengthOp
                        Id (name: L)
                     Id (name: i)
               Assignment (name: i)
                  BinaryOp (op: ADD)
                     Id (name: i)
                     IntLiteral (type: decimal, value: 1)
         Assignment (name: q)
            ClassInitialization (name: Lazysort)
         StatementExpression
            ClassMethod (name: quicksort)
               Id (name: q)
               Arguments
                  Argument
                     Id (name: L)
         Assignment (name: d)
            ClassMethod (name: printL)
               Id (name: q)
               Arguments
                  Argument
                     Id (name: L)
      InstanceMethodDeclaration (name: quicksort, type: boolean)
         Parameter (name: L, type: int[])
         VarDeclaration (name: lazy, type: boolean)
         VarDeclaration (name: rand, type: int)
         Assignment (name: rand)
            ClassMethod (name: random)
               Id (name: MathUtils)
               Arguments
                  Argument
                     IntLiteral (type: decimal, value: 0)
                  Argument
                     IntLiteral (type: decimal, value: 5)
         IfStatement
            Condition
               BinaryOp (op: LOW)
                  Id (name: rand)
                  IntLiteral (type: decimal, value: 4)
            StatementScope
               StatementExpression
                  ClassMethod (name: beLazy)
                     Id (name: this)
                     Arguments
                        Argument
                           Id (name: L)
               ArrayAssignment (name: L)
                  IntLiteral (type: decimal, value: 1)
                  ArrayAccess
*/