package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.specs.comp.ollir.Node;

public class InterferenceGraphCreator {
    Set<Pair<Set<Integer>, String>> webs;
    Map<String, Node> nodes;

    public InterferenceGraphCreator(Set<Pair<Set<Integer>, String>> webs){
        this.webs = webs;
        this.nodes = new HashMap<>();

        int id = 0;
        for(var pair : webs){
            Node node = new Node();
            node.setId(id++);
            nodes.put(pair.second, node);
        }
    }

    public Set<Pair<Node, String>> createGraph(){
        for(var firstPair : webs){
            for(var secondPair : webs){
                Node firstNode = nodes.get(firstPair.second);
                Node secondNode = nodes.get(secondPair.second);
                if(secondNode.getId() <= firstNode.getId()){
                    continue;
                }
                if(!Collections.disjoint(firstPair.first, secondPair.first)){
                    firstNode.addSucc(secondNode);
                    secondNode.addSucc(firstNode);
                }
            }
        }
        
        Set<Pair<Node, String>> graph = new HashSet<>();
        for(var entry : this.nodes.entrySet()){
            graph.add(new Pair<>(entry.getValue(), entry.getKey()));
            
            // DEBUG
            System.out.print(entry.getKey() + " - ");
            System.out.print(entry.getValue().getId());
            System.out.print(" succ:{");
            for(var s : entry.getValue().getSuccessors()){
                System.out.print(s.getId() + ", ");
            }
            System.out.println("}");

        }
        
        return graph;
    }
}
