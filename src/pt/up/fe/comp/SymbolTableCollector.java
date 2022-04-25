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
    }

    public Boolean visitProgram(JmmNode program, MapSymbolTable symbolTable) {
        for (var child : program.getChildren()) {
            visit(child, imports);
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
}