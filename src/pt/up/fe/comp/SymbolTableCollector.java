package pt.up.fe.comp;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import pt.up.fe.comp.MapSymbolTable;

public class SymbolTableCollector extends AJmmVisitor<MapSymbolTable, Boolean> {

    public SymbolTableCollector() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDecl", this::visitImportDecl);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        //addVisit("InstanceMethodDeclaration", this::)
    }

    public Boolean visitProgram(JmmNode program, MapSymbolTable symbolTable) {
        for (var child : program.getChildren()) {
            visit(child, symbolTable);
        }
        return true;
    }

    private Boolean visitImportDecl(JmmNode importDecl, MapSymbolTable symbolTable) {
        var packageString = importDecl.get("name");
        var subpackagesString = importDecl.getChildren().stream()
                .map(id -> id.get("name"))
                .collect(Collectors.joining("."));
        var importString = packageString + (subpackagesString.isEmpty() ? "" : "." + subpackagesString);
        symbolTable.addImport(importString);
        return true;
    }

    private Boolean visitClassDeclaration(JmmNode classDeclaration, MapSymbolTable symbolTable) {
        symbolTable.setClassName(classDeclaration.get("className"));
        symbolTable.setSuperName(classDeclaration.get("baseClassName"));

        for (var child : classDeclaration.getChildren()) {
            if (child.getKind() == "VarDeclaration"){
                visitFieldDeclaration(child, symbolTable);
            } else {
                visit(child, symbolTable);
            }
        }

        return true;
    }

    private Boolean visitFieldDeclaration(JmmNode varDeclaration, MapSymbolTable symbolTable) {
        var type = varDeclaration.get("type");
        symbolTable.addField(new Symbol(new Type(varDeclaration.get("type"), false), varDeclaration.get("name")));
        return true;
    }

    private Boolean visitVarDeclaration(JmmNode varDeclaration, Method method){
        return true;
    }

    private Boolean visitMainMethodDeclaration(JmmNode mainMethodDeclaration, MapSymbolTable symbolTable) {
        System.out.println("inside method");
        return true;
    }
}