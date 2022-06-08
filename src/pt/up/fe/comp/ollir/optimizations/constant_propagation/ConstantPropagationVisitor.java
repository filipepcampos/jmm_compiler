package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class ConstantPropagationVisitor extends AJmmVisitor<Boolean, Boolean> {
    public ConstantPropagationVisitor(){
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
        ConstantPropagationMethodVisitor visitor = new ConstantPropagationMethodVisitor("main");
        return visitor.visit(node, true);
    }

    private Boolean visitInstanceMethodDeclaration(JmmNode node, Boolean dummy){
        String methodSignature = node.get("name");
        ConstantPropagationMethodVisitor visitor = new ConstantPropagationMethodVisitor(methodSignature);
        return visitor.visit(node, true);
    }
    
}
