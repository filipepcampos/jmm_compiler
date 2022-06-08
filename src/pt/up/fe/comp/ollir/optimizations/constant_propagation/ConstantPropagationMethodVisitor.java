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
        addVisit(AstNode.WHILE_STATEMENT, this::visitWhileStatement);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean defaultVisit(JmmNode node, Boolean dummy){
        boolean updated = false;
        for(var stmt : node.getChildren()) {
            updated |= visit(stmt, true);
        }
        return updated;
    }

    private boolean visitAssignment(JmmNode node, Boolean dummy){
        String name = node.get("name");
        JmmNode childNode = node.getJmmChild(0);
        visit(childNode);
        childNode = node.getJmmChild(0);
        String childNodeKind = childNode.getKind();
        if(childNodeKind.equals("IntLiteral") || childNodeKind.equals("Bool")){
            System.out.println("Setting " + name + ": " + childNode);
            JmmNode newNode = new JmmNodeImpl(childNodeKind);
            newNode.put("value", childNode.get("value"));
            childNode.getOptional("type").ifPresent(t -> newNode.put("type", t));
            constantMap.put(name, newNode);
        } else {
            if(constantMap.containsKey(name)){
                System.out.println("Unsetting " + name);
                constantMap.remove(name);
            }
        }
        return false;
    }

    private boolean visitId(JmmNode node, Boolean dummy){
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

    private boolean visitClassMethod(JmmNode node, Boolean dummy){
        boolean updated = false;

        JmmNode arguments = node.getJmmChild(1);
        for(int i = 0; i < arguments.getNumChildren(); ++i){
            updated |= visit(arguments.getJmmChild(i));
            JmmNode argument = arguments.getJmmChild(i);
            if(argument.getKind().equals("Id")){
                String name = argument.get("name");
                if(this.constantMap.containsKey(name)){ // Can't guarantee there's no side effects in method
                    this.constantMap.remove(name);
                }
            }
        }
        return updated;
    }

    private boolean visitWhileStatement(JmmNode node, Boolean dummy){
        JmmNode conditionChild = node.getJmmChild(0);
        JmmNode statements = node.getJmmChild(1);

        if(conditionChild.getKind().equals("Id")){
            String name = conditionChild.get("name");
            if(this.constantMap.containsKey(name) && !containsVariableUsage(statements, name)){
                visit(conditionChild); // This will swap the node with a const
                return true;
            }
        }
        return false; // TODO: Should while statementscope receive constant propagation?
    }

    private boolean containsVariableUsage(JmmNode node, String variableName){
        if(node.getKind().equals("Id")){
            return node.get("name").equals(variableName);
        }
        for(JmmNode child : node.getChildren()){
            if(containsVariableUsage(child, variableName)){
                return true;
            }
        }
        return false;
    }
}
