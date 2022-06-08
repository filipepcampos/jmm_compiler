package pt.up.fe.comp.ollir.optimizations.constant_propagation;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantPropagationMethodVisitor extends AJmmVisitor<Boolean, Boolean> {
    String methodSignature;
    Map<String, JmmNode> constantMap;

    public ConstantPropagationMethodVisitor(String methodSignature){
        this.methodSignature = methodSignature;
        this.constantMap = new HashMap<>();

        addVisit(AstNode.ID, this::visitId);
        addVisit(AstNode.ASSIGNMENT, this::visitAssignment);
        addVisit(AstNode.CLASS_METHOD, this::visitClassMethod);
        setDefaultVisit(this::defaultVisit);
    }

    public boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var stmt : node.getChildren()) {
            boolean visitResult = visit(stmt, true);
            updated |= visitResult;
        }
        return updated;
    }

    public boolean visitAssignment(JmmNode node, Boolean dummy){
        String name = node.get("name");
        JmmNode childNode = node.getJmmChild(0);
        visit(childNode);
        childNode = node.getJmmChild(0);
        String childNodeKind = childNode.getKind();
        if(childNodeKind.equals("IntLiteral") || childNodeKind.equals("Bool")){
            System.out.println("Setting " + name + ": " + childNode);
            JmmNode newNode = new JmmNodeImpl(childNodeKind);
            newNode.put("value", childNode.get("value"));
            if(childNode.get("type") != null){
                newNode.put("type", childNode.get("type"));
            }
            constantMap.put(name, newNode);
        } else {
            if(constantMap.containsKey(name)){
                System.out.println("Unsetting " + name);
                constantMap.remove(name);
            }
        }
        return false;
    }

    public boolean visitId(JmmNode node, Boolean dummy){
        String name = node.get("name");
        
        if(node.getJmmParent().getKind().equals("ClassMethod")){
            // If the node's parent is ClassMethod then id doesn't represent a variable name
            return false;
        }

        if(constantMap.containsKey(name)){
            node.replace(constantMap.get(name));
            return true;
        }

        return false;
    }

    public boolean visitClassMethod(JmmNode node, Boolean dummy){
        JmmNode arguments = node.getJmmChild(1);
        for(var argument : arguments.getChildren()){
            if(argument.getKind().equals("Id")){
                String name = argument.get("name");
                if(this.constantMap.containsKey(name)){ // Can't guarantee there's no side effects in method
                    this.constantMap.remove(name);
                }
            }
        }
        return false;
    }

    
}
