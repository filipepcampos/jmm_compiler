package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import pt.up.fe.comp.ImportCollector;
import pt.up.fe.comp.Method;

public class MapSymbolTable implements SymbolTable {
    List<String> importList;
    String className;
    String superName;
    Map<Symbol, Boolean> fields;
    Map<String, Method> methods;    // methodSignature -> Method Class    

    public MapSymbolTable(JmmNode rootNode) {
        ImportCollector importCollector = new ImportCollector();
        this.importList = new ArrayList<String>();
        importCollector.visitProgram(rootNode, this.importList);
    }

    public List<String> getImports(){
        return this.importList;
    }

    public void addImport(String importName){
        this.importList.add(importName);
    }

    public String getClassName(){
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuper(){
        return this.superName;
    }

    public void setSuperName(String superName) {
        this.superName = superName;
    }
    
    public List<Symbol> getFields(){
        return new ArrayList<Symbol>();
    }

    public void addField(Symbol field) {
        this.fields.put(field, false);
    }

    public List<String> getMethods(){
        return new ArrayList<String>(this.methods.keySet());
    }

    public Type getReturnType(String methodSignature) {
        return this.methods.get(methodSignature).getReturnType();
    }

    public List<Symbol> getParameters(String methodSignature) {
        return new ArrayList<Symbol>();
    }

    public List<Symbol> getLocalVariables(String methodSignature){
        return this.methods.get(methodSignature).getLocalVariables();
    }
}