package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantPropagationVisitor extends AJmmVisitor<Boolean, Boolean> {
    SymbolTable symbolTable;

    public ConstantPropagationVisitor(SymbolTable symbolTable){
        this.symbolTable = symbolTable;
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitInstanceMethodDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var child : node.getChildren()){
            updated |= visit(child, true);
        }
        return updated;
    }

    private Boolean visitMainMethodDeclaration(JmmNode node, Boolean dummy){
        ConstantPropagationMethodVisitor visitor = new ConstantPropagationMethodVisitor(this.symbolTable, "main");
        return visitor.visit(node, true);
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, Boolean dummy){
        ConstantPropagationMethodVisitor visitor = new ConstantPropagationMethodVisitor(this.symbolTable, node.get("name"));
        return visitor.visit(node, true);
    }
}
