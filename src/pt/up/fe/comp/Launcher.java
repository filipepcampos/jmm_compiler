package pt.up.fe.comp;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.analysis.JmmAnalyser;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.comp.SymbolTableCollector;
import pt.up.fe.comp.MapSymbolTable;
import pt.up.fe.comp.JasminGenerator;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Launcher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // read the input code
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }
        File inputFile = new File(args[0]);
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + args[0] + "'.");
        }
        String input = SpecsIo.read(inputFile);

        
        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(input, "Program", config);

        // Check if there are parsing errors
        parserResult.getReports().stream()
            .filter(report -> report.getType() == ReportType.ERROR)
            .findFirst()
            .ifPresent(report -> {
                if (report.getException().isPresent()) {
                    System.out.println(report.getMessage());
                }
            });

        JmmNode rootNode = parserResult.getRootNode();
        if (rootNode != null) {
            System.out.println(rootNode.toTree());
        } else {
            System.out.println("Program finished due to parser error.");
            return;
        }
        
        JmmAnalyser analyser = new JmmAnalyser();
        JmmSemanticsResult analysisResult = analyser.semanticAnalysis(parserResult);
        System.out.println(analysisResult.getSymbolTable().print());
        TestUtils.noErrors(analysisResult);

        MapSymbolTable symbolTable = new MapSymbolTable();
        SymbolTableCollector collector = new SymbolTableCollector();
        collector.visit(rootNode, symbolTable);
        System.out.println(symbolTable.print());
 
        String content = SpecsIo.read("./test/fixtures/public/ollir/myclass3.ollir");
        System.out.println(content);
        OllirResult ollirResult = new OllirResult(content, new HashMap<String, String>());

        JasminGenerator generator = new JasminGenerator();
        generator.toJasmin(ollirResult);

        // ... add remaining stages
    }

}
