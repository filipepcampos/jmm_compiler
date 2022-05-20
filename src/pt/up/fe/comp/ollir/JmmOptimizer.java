package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        try {
            ollirGenerator.visit(semanticsResult.getRootNode());
        } catch(Exception e){
            // Might occur due to unsupported features like method overloading
            List<Report> reports = new ArrayList<>();
            reports.add(new Report(ReportType.ERROR, Stage.LLIR, -1, "OLLIR parse exception occurred."));
            // OllirResult cannot be created with invalid ollir code
            return null; 

        }
        String ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR code:\n");
        printOllirCode(ollirCode);

        OllirResult result;
        try {
            result = new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
        } catch(Exception e){
            List<Report> reports = new ArrayList<>();
            reports.add(new Report(ReportType.ERROR, Stage.LLIR, -1, "OLLIR parse exception occurred."));
            // Code may use a feature that's not been implemented yet and doesn't generate correct ollirCode for it
            // TODO: Remove in the end of the project
            result = null;
        }

        return result;
    }

    // Prints ollircode with indentation
    private void printOllirCode(String ollirCode){
        int indent = 0;
        boolean indentNextLine = false;
        for(char c : ollirCode.toCharArray()){
            if(c == '\n'){
                System.out.println();
                indentNextLine = true;
            } else if(c == '}'){
                indent--;
                System.out.print(" ".repeat(indent*2));
                System.out.print(c);
                indentNextLine = false;
            } else {
                if(indentNextLine){
                    System.out.print(" ".repeat(indent*2));
                    indentNextLine = false;
                }
                System.out.print(c);
            }
            if(c == '{')
                indent++;
        }
    }
}
