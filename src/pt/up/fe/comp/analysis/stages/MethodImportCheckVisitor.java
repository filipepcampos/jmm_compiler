package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.analysis.JmmType;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.List;

public class MethodImportCheckVisitor extends PreorderJmmVisitor<List<Report>, Boolean> {
    String methodSignature;
    SymbolTable symbolTable;
    List<String> imports;

    public MethodImportCheckVisitor(SymbolTable symbolTable, String methodSignature, List<String> imports) {
        this.symbolTable = symbolTable;
        this.methodSignature = methodSignature;
        this.imports = imports;
        addVisit("Parameter", this::visitDeclaration);
        addVisit("VarDeclaration", this::visitDeclaration);
    }

    private Boolean visitDeclaration(JmmNode node, List<Report> reports){
        String type = node.get("type");
        Boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        if(type.equals("int") || type.equals("boolean") || type.equals("String")){
            return true;
        }
        if(!imports.contains(type) && !type.equals(symbolTable.getClassName())){
            Report debug = new Report(ReportType.ERROR, Stage.SEMANTIC,
            Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
            "Class " + type + " has not been imported.");
            reports.add(debug);
        }
        return true;
    }

}