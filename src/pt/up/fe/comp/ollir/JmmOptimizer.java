package pt.up.fe.comp.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        OllirGenerator ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        String ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR code:\n");
        printOllirCode(ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
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
