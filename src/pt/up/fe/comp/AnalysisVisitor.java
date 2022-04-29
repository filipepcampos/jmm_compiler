package pt.up.fe.comp;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.report.Report;
import java.util.List;

public class AnalysisVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;

    public AnalysisVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("InstanceMethodDeclaration", this::visitInstanceMethodDeclaration);
    }

    public Boolean visitMainMethodDeclaration(JmmNode node, List<Report> reports){
        MethodAnalysisVisitor visitor = new MethodAnalysisVisitor(symbolTable, "main");
        visitor.visit(node, reports);
        return true;
    }

    public Boolean visitInstanceMethodDeclaration(JmmNode node, List<Report> reports){
        String methodSignature = node.get("name");
        MethodAnalysisVisitor visitor = new MethodAnalysisVisitor(symbolTable, methodSignature);
        visitor.visit(node, reports);
        return true;
    }
}
