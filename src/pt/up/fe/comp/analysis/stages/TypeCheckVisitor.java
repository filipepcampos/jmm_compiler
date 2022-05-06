package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import java.util.List;

public class TypeCheckVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;

    public TypeCheckVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("InstanceMethodDeclaration", this::visitInstanceMethodDeclaration);
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, List<Report> reports){
        MethodTypeCheckVisitor visitor = new MethodTypeCheckVisitor(symbolTable, "main");
        visitor.visit(node, reports);
        return true;
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, List<Report> reports){
        String methodSignature = node.get("name");
        MethodTypeCheckVisitor visitor = new MethodTypeCheckVisitor(symbolTable, methodSignature);
        visitor.visit(node, reports);
        return true;
    }
}
