package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

public class AnalysisUtils {
    public static boolean isNativeType(SymbolTable table, Type type){
        String typeName = type.getName();
        if(typeName.equals(table.getClassName()) || typeName.equals(table.getSuper())){
            return true;
        }
        if(typeName.equals("int") || typeName.equals("bool") || typeName.equals("String")){
            return true;
        }
        return false;
    }
}
