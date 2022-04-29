package pt.up.fe.comp;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult; 
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.AnalysisVisitor;
import pt.up.fe.comp.MapSymbolTable;
import pt.up.fe.comp.SymbolTableCollector;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
 
public class JmmAnalyser implements JmmAnalysis { 
    @Override 
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {         
        MapSymbolTable symbolTable = new MapSymbolTable(); 
        SymbolTableCollector collector = new SymbolTableCollector();
        collector.visit(parserResult.getRootNode(), symbolTable);

        AnalysisVisitor analysisVisitor = new AnalysisVisitor(symbolTable);
        List<Report> reports = new ArrayList<>();
        analysisVisitor.visit(parserResult.getRootNode(), reports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports); 
    } 
}