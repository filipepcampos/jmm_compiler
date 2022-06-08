package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantPropagationVisitor extends AJmmVisitor<Boolean, Boolean> {
    public ConstantPropagationVisitor(){
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitMethodDeclaration);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var child : node.getChildren()){
            updated |= visit(child, true);
        }
        return updated;
    }

    private Boolean visitMethodDeclaration(JmmNode node, Boolean dummy){
        ConstantPropagationMethodVisitor visitor = new ConstantPropagationMethodVisitor();
        return visitor.visit(node, true);
    }
}
