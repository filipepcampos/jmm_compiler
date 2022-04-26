package pt.up.fe.comp;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;

public class MethodCollector extends AJmmVisitor<Boolean, Boolean> {
    
    private JmmMethod method;

    public MethodCollector(JmmNode rootNode) {
        addVisit("MainMethodDeclaration", this::visitMainMethodDeclaration);
        addVisit("InstanceMethodDeclaration", this::visitMethodDeclaration);
        addVisit("Parameter", this::visitParameter);
        //addVisit("VarDeclaration", );
        addVisit("VarDeclaration", this::visitLocalVariable);

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
        Boolean isArray = typeName.endsWith("[]");
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
        Boolean isArray = type.endsWith("[]");
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