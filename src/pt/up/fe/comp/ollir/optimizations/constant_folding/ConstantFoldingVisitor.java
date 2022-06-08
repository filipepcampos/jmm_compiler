package pt.up.fe.comp.ollir.optimizations.constant_folding;

import java.util.Optional;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantFoldingVisitor extends AJmmVisitor<Boolean, Boolean> {
    public ConstantFoldingVisitor(){
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
        return visitMethodDeclaration(node, "main");
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, Boolean dummy){
        return visitMethodDeclaration(node, node.get("name"));
    }    

    private Boolean visitMethodDeclaration(JmmNode node, String methodSignature){
        ConstantFoldingMethodVisitor visitor = new ConstantFoldingMethodVisitor(methodSignature);
        visitor.visit(node, true);
        return visitor.wasUpdated();
    }
}
