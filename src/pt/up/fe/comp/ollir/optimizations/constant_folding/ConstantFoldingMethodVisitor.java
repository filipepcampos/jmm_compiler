package pt.up.fe.comp.ollir.optimizations.constant_folding;

import java.util.Optional;

import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

public class ConstantFoldingMethodVisitor extends AJmmVisitor<Boolean, Optional<Integer>> {
    Boolean updated;

    public ConstantFoldingMethodVisitor(){
        this.updated = false;
        
        addVisit(AstNode.INT_LITERAL, this::visitIntLiteral);
        addVisit(AstNode.BOOL, this::visitBool);
        addVisit(AstNode.UNARY_OP, this::visitUnaryOp);
        addVisit(AstNode.BINARY_OP, this::visitBinaryOp);
        setDefaultVisit(this::defaultVisit);
    }

    private Optional<Integer> defaultVisit(JmmNode node, Boolean dummy){
        for(var stmt : node.getChildren()) {
            visit(stmt, true);
        }
        return Optional.empty();
    }

    private Optional<Integer> visitIntLiteral(JmmNode node, Boolean dummy){
        String type = node.get("type");
        String stringValue = node.get("value");
        int value = 0;
        switch(type){
            case "decimal": value = Integer.parseInt(stringValue);
                break;
            case "binary": value = Integer.parseInt(stringValue, 2);
                break;
            case "octal": value = Integer.parseInt(stringValue, 8);
                break;
            case "hexadecimal": value = Integer.parseInt(stringValue, 16);
                break;
        }
        return Optional.of(value);
    }

    private Optional<Integer> visitBool(JmmNode node, Boolean dummy){
        String stringValue = node.get("value");
        Integer value = stringValue.equals("true") ? 1 : 0;
        return Optional.of(value);
    }

    private Optional<Integer> visitUnaryOp(JmmNode node, Boolean dummy){
        JmmNode child = node.getJmmChild(0);
        Optional<Integer> optional = visit(child);
        
        if(optional.isPresent()){
            node.removeJmmChild(child);
            JmmNode newChild = new JmmNodeImpl("Bool");
            newChild.put("value",  Integer.toString(1 - optional.get())); // !
            node.replace(newChild);
            updated = true;
        }
        return optional;
    }

    private Optional<Integer> visitBinaryOp(JmmNode node, Boolean dummy){
        String op = node.get("op");
        
        Optional<Integer> firstChild = visit(node.getJmmChild(0));
        Optional<Integer> secondChild = visit(node.getJmmChild(1));

        if(firstChild.isEmpty() || secondChild.isEmpty()){
            return Optional.empty();
        }

        JmmNode newNode = null;
        Integer value = 0;
        switch(op){
            case "AND":
                newNode = new JmmNodeImpl("Bool");
                value = firstChild.get() & secondChild.get();
                break;
            case "LOW":
                newNode = new JmmNodeImpl("Bool");
                value = firstChild.get() < secondChild.get() ? 1 : 0;
                break;
            case "ADD":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() + secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "SUB":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() - secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "MUL":
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() * secondChild.get();
                newNode.put("type", "decimal");
                break;
            case "DIV": 
                newNode = new JmmNodeImpl("IntLiteral");
                value = firstChild.get() / secondChild.get();
                newNode.put("type", "decimal");
                break;
        }

        if(node != null){
            newNode.put("value", Integer.toString(value));
            node.replace(newNode);
            updated = true;
            return Optional.of(value);
        }
        return Optional.empty();
    }

    public Boolean wasUpdated() {
        return updated;
    }
    
}
