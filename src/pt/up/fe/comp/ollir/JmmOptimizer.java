package pt.up.fe.comp.ollir;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp.ollir.optimizations.constant_folding.ConstantFoldingMethodVisitor;
import pt.up.fe.comp.ollir.optimizations.constant_folding.ConstantFoldingVisitor;
import pt.up.fe.comp.ollir.optimizations.constant_propagation.ConstantPropagationVisitor;
import pt.up.fe.comp.ollir.optimizations.unused_assignment_removing.UnusedAssignmentRemoverVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.OllirErrorException;

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
            throw(e);
            // OllirResult cannot be created with invalid ollir code
            //return null;

        }
        String ollirCode = ollirGenerator.getCode();

        System.out.println("OLLIR code:\n");
        printOllirCode(ollirCode);

        OllirResult result;
        result = new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
        /*
        try {
            result = new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
        } catch(Exception e){
            List<Report> reports = new ArrayList<>();
            reports.add(new Report(ReportType.ERROR, Stage.LLIR, -1, "OLLIR parse exception occurred."));
            // Code may use a feature that's not been implemented yet and doesn't generate correct ollirCode for it
            // TODO: Remove in the end of the project
            result = null;
        }*/

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

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode rootNode = semanticsResult.getRootNode();

        boolean updated;
        do {
            updated = false;

            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor();
            updated |= constantPropagationVisitor.visit(rootNode);

            System.out.println("c prop. " + updated);
            
            ConstantFoldingVisitor constantFoldingVisitor = new ConstantFoldingVisitor();
            updated |= constantFoldingVisitor.visit(rootNode);

            System.out.println("c fold. " + updated);
        } while(updated);

        UnusedAssignmentRemoverVisitor unusedAssignmentRemoverVisitor = new UnusedAssignmentRemoverVisitor(semanticsResult.getSymbolTable());
        unusedAssignmentRemoverVisitor.visit(rootNode);

        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        // DEBUG TODO: Remove
        
        for (Method method : ollirResult.getOllirClass().getMethods()) {
            method.buildCFG();
            try {
                method.outputCFG();
            } catch(OllirErrorException e){
                e.printStackTrace();
            }
            Node node = method.getBeginNode();  // TODO: Cast to instruction
        }

        return ollirResult;
    }
}
