package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    
    public LivenessAnalyser(Node beginNode){
        nodesList = new ArrayList<>();
        useList = new ArrayList<>();
        defList = new ArrayList<>();
        inList = new ArrayList<>();
        outList = new ArrayList<>();

        this.addNode(beginNode);
        this.initNodes();
        this.debugPrint();
        this.livenessAnalyse();
    }

    private void debugPrint() {// TODO Remove
        System.out.println("LivenessAnalyser: ");
        for(int i = 0; i < nodesList.size(); ++i){
            System.out.print(((Instruction) nodesList.get(i)).getId());
            System.out.print(" - use:{");
            for(var u : useList.get(i)){
                System.out.print(u + ", ");
            }
            System.out.print("} def:{");
            for(var d : defList.get(i)){
                System.out.print(d + ", ");
            }
            System.out.print("} in:{");
            for(var j : inList.get(i)){
                System.out.print(j + ", ");
            }
            System.out.print("} out:{");
            for(var o : outList.get(i)){
                System.out.print(o + ", ");
            }
            System.out.println("}");
        }
        System.out.println("\n");
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
                    def.add(((Operand) assignInstruction.getDest()).getName());
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

        /*
        for each n
            in[n] <- {}; out[n] <- {}
        repeat
            for each n
                in’[n] <- in[n]; out’[n] <- out[n]
                out[n] <- U succ in[s]

                in[n] <- use[n] U (out[n] – def[n])
        until in’[n] = in[n] and out’[n] = out[n] for all n
        */
       
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
            this.debugPrint();
        } while(updated);
    }

    private void createWebs(){
        
    }
}
