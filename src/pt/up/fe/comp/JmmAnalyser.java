package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult; 
import pt.up.fe.comp.MapSymbolTable;
import pt.up.fe.comp.SymbolTableCollector;
import java.util.Collections;
 
public class JmmAnalyser implements JmmAnalysis { 
    @Override 
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {         
        MapSymbolTable symbolTable = new MapSymbolTable(); 
        SymbolTableCollector collector = new SymbolTableCollector();
        collector.visit(parserResult.getRootNode(), symbolTable);
        return new JmmSemanticsResult(parserResult, symbolTable, Collections.emptyList()); 
    } 
}