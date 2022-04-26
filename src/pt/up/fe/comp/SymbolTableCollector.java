package pt.up.fe.comp;
import pt.up.fe.comp.MapSymbolTable;
import pt.up.fe.comp.JmmMethod;
import pt.up.fe.comp.MethodCollector;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.stream.Collectors;
import java.util.Optional;

public class SymbolTableCollector extends AJmmVisitor<MapSymbolTable, Boolean> {

    public SymbolTableCollector() {
        addVisit("Program", this::visitProgram);
        addVisit("ImportDecl", this::visitImportDecl);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("MainMethodDeclaration", this::visitMethodDeclaration);
        addVisit("InstanceMethodDeclaration", this::visitMethodDeclaration);
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
        Optional<String> baseClassName = classDeclaration.getOptional("baseClassName");
        if(baseClassName.isPresent()){
            symbolTable.setSuperName(classDeclaration.get("baseClassName"));
        }

        for (var child : classDeclaration.getChildren()) {
            visit(child, symbolTable);
        }

        return true;
    }

    private Boolean visitVarDeclaration(JmmNode varDeclaration, MapSymbolTable symbolTable){
        String type = varDeclaration.get("type");
        Boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        symbolTable.addField(new Symbol(new Type(type, isArray), varDeclaration.get("name")));
        return true;
    }

    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, MapSymbolTable symbolTable) {
        MethodCollector collector = new MethodCollector(methodDeclaration);
        symbolTable.addMethod(collector.getMethod());
        return true;
    }
}