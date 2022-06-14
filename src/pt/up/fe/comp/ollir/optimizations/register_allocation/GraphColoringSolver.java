package pt.up.fe.comp.ollir.optimizations.register_allocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.specs.comp.ollir.Node;

/*
 * 
 * Coloring a graph with N colors
• If degree < N (degree of a node = # of edges)
– Node can always be colored
– After coloring the rest of the nodes, you’ll have at least one color
left to color the current node
• If degree ≥ N
– still may be colorable with N colors
– exact solution is NP complete

• Remove nodes that have degree < N
– Push the removed nodes onto a stack
• If all the nodes have degree ≥ N
– Find a node to spill (no color for that node)
– Remove that node
• When empty, start the coloring step
– pop a node from stack back
– Assign it a color that is different from its connected nodes (since
degree < N, a color should exist)
 */


public class GraphColoringSolver {
    Set<Pair<Node, String>> nodes;
    Map<Node, Integer> colors;
    Map<String, Integer> variableColorMap;
    int numberOfColors;

    public GraphColoringSolver(Set<Pair<Node, String>> nodes, int numberOfColors) {
        this.nodes = nodes;
        this.numberOfColors = numberOfColors;
        this.colors = new HashMap<>();
        this.variableColorMap = new HashMap<>();
    }

    public boolean solve() {
        Stack<Pair<Node, String>> stack = new Stack<>();
        System.out.println("Solving graph coloring there's " + this.nodes.size() + " nodes.");
        for(var node : this.nodes){
            if(node.first.getSuccessors().size() >= this.numberOfColors){
                return false;
            }
            stack.push(node);
        }

        while(!stack.isEmpty()){
            Pair<Node, String> pair = stack.pop();
            Node node = pair.first;
            
            String[] splitName = pair.second.split("-");
            String name = splitName[0]; // Name without web id
            
            if(this.variableColorMap.containsKey(name)){
                continue;
            }
            
            Set<Integer> usedColors = new HashSet<>();
            System.out.println("Stack removing node " + node.getId());
            for (var succ : node.getSuccessors()){
                if(this.colors.containsKey(succ)){
                    usedColors.add(this.colors.get(succ));
                }
            }

            int color = 0;
            while(true){
                if(!usedColors.contains(color)){
                    break;
                }
                color++;
            }
            
            System.out.println("Coloring with " + color);
            this.colors.put(node, color);
            this.variableColorMap.put(name, color);
        }

        System.out.println("All colors:");
        for(var entry : this.colors.entrySet()){
            System.out.println(entry.getKey().getId() + " , color:" + entry.getValue());
        }
        System.out.println("Variable to color:");
        for(var entry : this.variableColorMap.entrySet()){
            System.out.println(entry.getKey() + "-> " + entry.getValue());
        }

        return true;
    }
}