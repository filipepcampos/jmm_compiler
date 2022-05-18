package pt.up.fe.comp.analysis.table;
import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.ast.AstNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;

public class MethodCollector extends AJmmVisitor<Boolean, Boolean> {
    
    private JmmMethod method;

    public MethodCollector(JmmNode rootNode) {
        addVisit(AstNode.MAIN_METHOD_DECLARATION, this::visitMainMethodDeclaration);
        addVisit(AstNode.INSTANCE_METHOD_DECLARATION, this::visitMethodDeclaration);
        addVisit(AstNode.PARAMETER, this::visitParameter);
        addVisit(AstNode.VAR_DECLARATION, this::visitLocalVariable);

        visit(rootNode);
    }

    public JmmMethod getMethod() {
        return this.method;
    }

    private Boolean visitMainMethodDeclaration(JmmNode mainMethodDeclaration, Boolean dummy) {
        this.method = new JmmMethod("main", new Type("void", false));
        for (var child : mainMethodDeclaration.getChildren()) {
            visit(child);
        }
        return true;
    }

    private Boolean visitMethodDeclaration(JmmNode methodDeclaration, Boolean dummy) {
        String signature = methodDeclaration.get("name");
        String typeName = methodDeclaration.get("type");
        boolean isArray = typeName.endsWith("[]");
        if (isArray) {
            typeName = typeName.substring(0, typeName.length() - 2);
        }
        Type type = new Type(typeName, isArray);
        this.method = new JmmMethod(signature, type);
        for (var child : methodDeclaration.getChildren()) {
            visit(child);
        }
        return true;
    }
    
    private Symbol varDeclarationToSymbol(JmmNode varDeclaration){
        String type = varDeclaration.get("type");
        boolean isArray = type.endsWith("[]");
        if (isArray) {
            type = type.substring(0, type.length() - 2);
        }
        return new Symbol(new Type(type, isArray), varDeclaration.get("name"));
    }
    
    private Boolean visitLocalVariable(JmmNode varDeclaration, Boolean dummy) {
        this.method.addLocalVariable(this.varDeclarationToSymbol(varDeclaration));
        return true;
    }

    private Boolean visitParameter(JmmNode parameterDeclaration, Boolean dummy) {
        this.method.addParameter(this.varDeclarationToSymbol(parameterDeclaration));
        return true;
    }
}