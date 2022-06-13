package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.specs.comp.ollir.AssignInstruction;
import org.specs.comp.ollir.BinaryOpInstruction;
import org.specs.comp.ollir.CallInstruction;
import org.specs.comp.ollir.CallType;
import org.specs.comp.ollir.CondBranchInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.ElementType;
import org.specs.comp.ollir.Instruction;
import org.specs.comp.ollir.Node;
import org.specs.comp.ollir.NodeType;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.GetFieldInstruction;
import org.specs.comp.ollir.PutFieldInstruction;
import org.specs.comp.ollir.UnaryOpInstruction;
import org.specs.comp.ollir.ReturnInstruction;
import org.specs.comp.ollir.SingleOpInstruction;

public class LivenessAnalyser {
    private List<Node> nodesList;
    private List<Set<String>> useList;
    private List<Set<String>> defList;
    private List<Set<String>> inList;
    private List<Set<String>> outList;
    private HashMap<String, Set<Set<Integer>>> webs;
    
    public LivenessAnalyser(Node beginNode){
        this.nodesList = new ArrayList<>();
        this.useList = new ArrayList<>();
        this.defList = new ArrayList<>();
        this.inList = new ArrayList<>();
        this.outList = new ArrayList<>();
        this.webs = new HashMap<>();

        this.addNode(beginNode);
        this.initNodes();
        this.livenessAnalyse();
        this.createWebs();
    }

    private void addNode(Node node){
        if (!nodesList.contains(node)){
            if (node.getNodeType() == NodeType.INSTRUCTION){
                nodesList.add(node);
            }
            for(Node succ : node.getSuccessors()){
                addNode(succ);
            }
        }
    }

    private void initNodes() {
        for(Node node : nodesList){
            if(node.getNodeType() == NodeType.INSTRUCTION){
                Instruction instruction = (Instruction) node;

                // If assign add to def
                Set<String> def = new HashSet<>();
                if (instruction instanceof AssignInstruction) {
                    AssignInstruction assignInstruction = (AssignInstruction) instruction;
                    String variableName = ((Operand) assignInstruction.getDest()).getName();
                    def.add(variableName);
                    this.webs.put(variableName, new HashSet<>());   // Everytime a variable is defined a new entry in webs is created
                }
                defList.add(def);

                // For any instruction collectUses
                Set<String> use = new HashSet<>();
                this.collectUses(instruction, use);
                useList.add(use);

                // Initialize in and out to empty set
                inList.add(new HashSet<>());
                outList.add(new HashSet<>());
            }
        }
    }

    private void collectUses(Instruction instruction, Set<String> use){
        switch (instruction.getInstType()) {
            case ASSIGN:
                AssignInstruction assignInstruction = (AssignInstruction) instruction;
                this.collectUses(assignInstruction.getRhs(), use);
                break;
            case BINARYOPER:
                BinaryOpInstruction binOp = (BinaryOpInstruction) instruction;
                Element lhsOperand = binOp.getLeftOperand();
                if(!lhsOperand.isLiteral()){
                    use.add(((Operand) lhsOperand).getName());
                }
                Element rhsOperand = binOp.getRightOperand();
                if(!rhsOperand.isLiteral()){
                    use.add(((Operand) rhsOperand).getName());
                }
                break;
            case BRANCH:
                CondBranchInstruction condInstruction = (CondBranchInstruction) instruction;
                this.collectUses(condInstruction.getCondition(), use);
                break;
            case CALL:
                CallInstruction callInstruction = (CallInstruction) instruction;
                Operand firstOperand = (Operand) callInstruction.getFirstArg();
                if(firstOperand.getType().getTypeOfElement() != ElementType.THIS && callInstruction.getInvocationType() != CallType.invokestatic){
                    use.add(firstOperand.getName());
                }
                for(var operand : callInstruction.getListOfOperands()){
                    if(!operand.isLiteral()) {
                        use.add(((Operand) operand).getName());
                    }
                }
                break;
            case GETFIELD:
                GetFieldInstruction getFieldInstruction = (GetFieldInstruction) instruction;
                Element getFieldClass = getFieldInstruction.getFirstOperand();
                if(getFieldClass.getType().getTypeOfElement() != ElementType.THIS){
                    use.add(((Operand) getFieldClass).getName());
                }
                break;
            case NOPER:
                SingleOpInstruction singleOpInstruction = (SingleOpInstruction) instruction;
                Element singleOperand = singleOpInstruction.getSingleOperand();
                if (!singleOperand.isLiteral()) {
                    use.add(((Operand) singleOperand).getName());
                }
                break;
            case PUTFIELD:
                PutFieldInstruction putFieldInstruction = (PutFieldInstruction) instruction;
                Element putFieldOperand = putFieldInstruction.getFirstOperand();
                if(putFieldOperand.getType().getTypeOfElement() != ElementType.THIS){
                    use.add(((Operand) putFieldOperand).getName());
                }
                Element putFieldInstructionThirdOperand = putFieldInstruction.getThirdOperand();
                if(!putFieldInstructionThirdOperand.isLiteral()){
                    use.add(((Operand) putFieldInstructionThirdOperand).getName());
                }
                break;
            case RETURN:
                ReturnInstruction returnInstruction = (ReturnInstruction) instruction;
                if (returnInstruction.hasReturnValue()) {
                    Element returnOperand = returnInstruction.getOperand();
                    if(!returnOperand.isLiteral()) {
                        use.add(((Operand) returnOperand).getName());
                    }
                }
                break;
            case UNARYOPER:
                UnaryOpInstruction unaryOpInstruction = (UnaryOpInstruction) instruction;
                Element unaryOpOperand = unaryOpInstruction.getOperand();
                if(!unaryOpOperand.isLiteral()){
                    use.add(((Operand) unaryOpInstruction.getOperand()).getName());    
                }
                break;
            default:
                break;
        }
    }

    private void livenessAnalyse() {
        boolean updated;
        do{
            updated = false;
            for(int i = 0; i < nodesList.size(); ++i){
                Node node = nodesList.get(i);
                Set<String> in = this.inList.get(i);
                Set<String> out = this.outList.get(i);
                Set<String> newIn = new HashSet<>();
                Set<String> newOut = new HashSet<>();

                for(Node succ : node.getSuccessors()){
                    int listIndex = this.nodesList.indexOf(succ);
                    if(listIndex != -1){
                        newOut.addAll(this.inList.get(listIndex));
                    }
                }

                newIn.addAll(this.useList.get(i));

                Set<String> outWithoutDef = new HashSet<>(); // out[i] - def[i]
                outWithoutDef.addAll(newOut);
                outWithoutDef.removeAll(this.defList.get(i));

                newIn.addAll(outWithoutDef);

                if(!(in.equals(newIn) && out.equals(newOut))){
                    updated = true;
                }
                
                this.inList.set(i, newIn);
                this.outList.set(i, newOut);
            }
        } while(updated);
    }

    private void createWebs(){
        for (var entry : this.webs.entrySet()) {
            for(int i = 0; i < this.nodesList.size(); ++i){
                Node node = nodesList.get(i);
                Set<String> def = this.defList.get(i);
                Set<Integer> web = new HashSet<>();
                if (def.contains(entry.getKey())){
                    this.propagateWeb(node, web, entry.getKey());
                    entry.getValue().add(web);
                }
            }

            System.out.print(entry.getKey() + " - ");
            for(var w : entry.getValue()){
                System.out.print("{");
                for(var k : w){
                    System.out.print(k + ", ");
                }
                System.out.print("}");
            }
            System.out.println();
            
            
            boolean updated;
            List<Set<Integer>> webs = new ArrayList<>(entry.getValue());
            do {
                updated = false;
                Set<Set<Integer>> killSet = new HashSet<>();
                
                for(int i = 0; i < webs.size(); ++i){
                    Set<Integer> firstWeb = webs.get(i);
                    for(int j = i + 1; j < webs.size(); ++j){
                        Set<Integer> secondWeb = webs.get(j);
                        if(!Collections.disjoint(firstWeb, secondWeb)){
                            secondWeb.addAll(firstWeb);
                            killSet.add(firstWeb);
                            updated = true;
                        }
                    }
                }
                for(var w : killSet){
                    webs.remove(w);
                }
            } while(updated);
            this.webs.put(entry.getKey(), new HashSet<>(webs));

            /*
            boolean updated;
            do {
                updated = false;
                Set<Set<Integer>> newWebs = new HashSet<>();
                for(var web : this.webs.get(entry.getKey())){
                    if(newWebs.isEmpty()){
                        newWebs.add(web);
                    } else {
                        for(var newWeb : newWebs){
                            if(!Collections.disjoint(web, newWeb)){
                                newWeb.addAll(web);
                                updated = true;
                                break;
                            }
                        }
                        if (!updated){
                            newWebs.add(web);
                        }
                    }
                }
                this.webs.put(entry.getKey(), newWebs);
            } while(updated);*/
        }
        
        for(var entry : this.webs.entrySet()){
            System.out.print(entry.getKey() + " - ");
            for(var w : entry.getValue()){
                System.out.print("{");
                for(var k : w){
                    System.out.print(k + ", ");
                }
                System.out.print("}");
            }
            System.out.println();
        }
    }
    
    public Set<Pair<Set<Integer>, String>> getWebs(){
        Set<Pair<Set<Integer>, String>> webs = new HashSet<>();
        for(var entry : this.webs.entrySet()){
            int id = 0;
            for(var web : entry.getValue()){
                webs.add(new Pair<>(web, entry.getKey() + "-" + id++));
            }
        }
        return webs;
    }

    private void propagateWeb(Node node, Set<Integer> web, String variable){
        int nodeIndex = this.nodesList.indexOf(node);
        if(node.getNodeType() == NodeType.INSTRUCTION && !web.contains(node.getId())){
            boolean inOut = this.outList.get(nodeIndex).contains(variable);
            boolean used = this.useList.get(nodeIndex).contains(variable);
            if(used || inOut){
                web.add(node.getId());
                if(inOut){
                    for(Node succ : node.getSuccessors()){
                        this.propagateWeb(succ, web, variable);
                    }
                }
            }
        }
    }
}
