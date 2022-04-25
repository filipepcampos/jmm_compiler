package pt.up.fe.comp;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class ImportCollector extends AJmmVisitor<List<String>, Integer> {

    private int visits;

    public ImportCollector() {
        this.visits = 0;
        addVisit("Program", this::visitProgram);
        addVisit("ImportDecl", this::visitImportDecl);
        setDefaultVisit((node, imports) -> ++visits);
    }

    public Integer visitProgram(JmmNode program, List<String> imports) {   // TODO: Verify if it can be public
        for (var child : program.getChildren()) {
            visit(child, imports);
        }
        return ++visits;
    }

    private Integer visitImportDecl(JmmNode importDecl, List<String> imports) {
        var packageString = importDecl.get("name");
        var subpackagesString = importDecl.getChildren().stream()
                .map(id -> id.get("name"))
                .collect(Collectors.joining("."));
        var importString = packageString + (subpackagesString.isEmpty() ? "" : "." + subpackagesString);
        imports.add(importString);
        return ++visits;
    }
}