package pt.up.fe.comp;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

public class JmmMethod {
    String signature;
    Type returnType;
    List<Symbol> parameters;
    Map<Symbol, Boolean> localVariables;

    public JmmMethod(String signature, Type returnType){
        this.signature = signature;
        this.returnType = returnType;
        this.parameters = new ArrayList<>();
        this.localVariables = new HashMap<Symbol, Boolean>();
    }

    public String getSignature(){
        return this.signature;
    }

    public Type getReturnType(){
        return this.returnType;
    }

    public void addLocalVariable(Symbol symbol){
        this.localVariables.put(symbol, false);
    }
    
    public void addParameter(Symbol symbol){
        this.parameters.add(symbol);
    }

    public List<Symbol> getLocalVariables(){
        return new ArrayList<Symbol>(this.localVariables.keySet().stream().collect(Collectors.toList()));
    }

    public List<Symbol> getParameters(){
        return new ArrayList<Symbol>(this.parameters);
    }
}