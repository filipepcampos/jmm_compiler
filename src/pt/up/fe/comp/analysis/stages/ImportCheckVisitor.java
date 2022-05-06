package pt.up.fe.comp.analysis.stages;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class ImportCheckVisitor extends AJmmVisitor<List<Report>, Boolean> {
    SymbolTable symbolTable;
    List<String> imports;

    public ImportCheckVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.imports = new ArrayList<>();
        for(String name : symbolTable.getImports()){
            String[] splitImport = name.split(".", -1);
            String className = splitImport[splitImport.length - 1];
            System.out.println(className);
            imports.add(className);
        }
        
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("InstanceMethodDeclaration", this::visitInstanceMethodDeclaration);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, List<Report> reports){
        for(var child : node.getChildren()){
            visit(child, reports);
        }
        return true;
    }

    private Boolean visitClassDeclaration(JmmNode node, List<Report> reports){
        Optional<String> baseClassName = node.getOptional("baseClassName");
        baseClassName.ifPresent(name -> {
            System.out.println("Got the baseclassname: " + baseClassName);
            for(String s : imports){
                System.out.println("  " + s);
            }
            if(!imports.contains(name)){
                reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
                Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
                "Super class " + name + " has not been imported."));
            }
        });
        return true;
    }

    private Boolean visitVarDeclaration(JmmNode node, List<Report> reports){
        String type = node.get("type");
        if(type.equals("int") || type.equals("boolean")){
            return true;
        }
        if(!imports.contains(type) && !type.equals(symbolTable.getClassName())){
            reports.add(new Report(ReportType.ERROR, Stage.SEMANTIC,
            Integer.parseInt(node.get("line")), Integer.parseInt(node.get("col")),
            "Class " + type + " has not been imported."));
        }
        return true;
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, List<Report> reports){
        MethodImportCheckVisitor visitor = new MethodImportCheckVisitor(symbolTable, "main", imports);
        visitor.visit(node, reports);
        return true;
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, List<Report> reports){
        String methodSignature = node.get("name");
        MethodImportCheckVisitor visitor = new MethodImportCheckVisitor(symbolTable, methodSignature, imports);
        visitor.visit(node, reports);
        return true;
    }
}
