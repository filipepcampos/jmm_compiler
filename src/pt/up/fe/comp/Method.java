import java.util.Map;
import pt.up.fe.comp.jmm.analysis.table.Symbol;

public class Method {
    String signature;
    Type returnType;
    Map<Symbol, Boolean> localVariables;

    public Method(String signature, Type returnType){
        this.signature = signature;
        this.returnType = returnType;
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

    public List<Symbol> getLocalVariables(){
        return new ArrayList<String>(this.localVariables.keySet());
    }
}