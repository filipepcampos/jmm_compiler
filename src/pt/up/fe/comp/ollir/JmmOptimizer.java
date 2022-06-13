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
import pt.up.fe.comp.ollir.optimizations.if_while_removal.IfWhileRemoverVisitor;
import pt.up.fe.comp.ollir.optimizations.register_allocation.LivenessAnalyser;
import pt.up.fe.comp.ollir.optimizations.unused_assignment_removing.UnusedAssignmentRemoverVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        Map<String, String> config = semanticsResult.getConfig();
        
        if(config.getOrDefault("optimizeAll", "false").equals("true")){
            astOptimizeAll(semanticsResult);
        } else if(config.getOrDefault("optimize", "false").equals("true")){
            astOptimizeBasic(semanticsResult);
        } 

        return semanticsResult;
    }

    private void astOptimizeAll(JmmSemanticsResult semanticsResult){
        JmmNode rootNode = semanticsResult.getRootNode();

        boolean updated;
        int i = 1;
        do {
            updated = false;

            System.out.println("\nOptimization round " + i);
            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
            boolean result = constantPropagationVisitor.visit(rootNode);
            updated |= result;
            System.out.println("Constant propagation - " + result);
            
            ConstantFoldingVisitor constantFoldingVisitor = new ConstantFoldingVisitor();
            result = constantFoldingVisitor.visit(rootNode);
            updated |= result;
            System.out.println("Constant folding - " + result);

            IfWhileRemoverVisitor ifWhileRemoverVisitor = new IfWhileRemoverVisitor();
            result = ifWhileRemoverVisitor.visit(rootNode);
            updated |= result;
            System.out.println("If/While removal - " + result);

            i++;
        } while(updated);
    
        UnusedAssignmentRemoverVisitor unusedAssignmentRemoverVisitor = new UnusedAssignmentRemoverVisitor(semanticsResult.getSymbolTable());
        boolean result = unusedAssignmentRemoverVisitor.visit(rootNode);
        System.out.println("Unused Assignments removal - " + result);
    }

    private void astOptimizeBasic(JmmSemanticsResult semanticsResult){
        boolean updated = false;
        do {
            ConstantPropagationVisitor constantPropagationVisitor = new ConstantPropagationVisitor(semanticsResult.getSymbolTable());
            updated = constantPropagationVisitor.visit(semanticsResult.getRootNode());
        } while(updated);
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
            LivenessAnalyser livenessAnalyser = new LivenessAnalyser(node);
        }

        return ollirResult;
    }
}
