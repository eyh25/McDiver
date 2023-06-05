package diver;

import game.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import graph.ShortestPaths;


/** This is the place for your implementation of the {@code SewerDiver}.
 */
public class McDiver implements SewerDiver {

    /** See {@code SewerDriver} for specification. */
    @Override
    public void seek(SeekState state) {

        // DO NOT WRITE ALL THE CODE HERE. DO NOT MAKE THIS METHOD RECURSIVE.
        // Instead, write your method (it may be recursive) elsewhere, with a
        // good specification, and call it from this one.
        //
        // Working this way provides you with flexibility. For example, write
        // one basic method, which always works. Then, make a method that is a
        // copy of the first one and try to optimize in that second one.
        // If you don't succeed, you can always use the first one.
        //
        // Use this same process on the second method, scram.
        //A suggested first implementation that will always find the ring, but likely won't receive a
        //large bonus multiplier, is a depth-first walk. Some modification is necessary to make the
        //search better, in general.
        //Use a recursive implementation for depth-first search
        dfs(state, new HashSet<>());
    }

    /** Moves McDiver to the ring using a Depth-first search walk algorithm.
     * Runs dfs on the Node closest to the ring and keeps track of the visited states, using a set
     * "Visited" which contains the ids of the visited states.
     *
     * Backtracks if the current state is a dead end and there are still unvisited states.
     * */
    public void dfs(SeekState state, Set<Long> visited){
        if (state.distanceToRing() == 0) {
            return;
        }
        //visit current state and add it to visited
        visited.add(state.currentLocation());

        //find all neighbors of current state and corresponding distances to ring
        Collection<NodeStatus> neighbors = state.neighbors();

        // create a list of the neighbors
        List<NodeStatus> neighborList = new ArrayList<>(neighbors);

        //sort the list of neighbors by distance to ring
        Collections.sort(neighborList, Comparator.comparingInt(NodeStatus::getDistanceToRing));

        Long current = state.currentLocation();

        // for each unvisited neighbor in the list, recursively call dfs on the neighbor
        for (NodeStatus neighbor : neighborList) {
            if (!visited.contains(neighbor.getId())){

                state.moveTo(neighbor.getId());
                visited.add(neighbor.getId());

                //recursive call
                dfs(state, visited);

                //base case for when we are on the ring
                if (state.distanceToRing() == 0) {
                    return;
                }

                //case where we are not on the ring and we have to backtrack
                state.moveTo(current);
            }
        }
    }

    /** See {@code SewerDriver} for specification. */
    @Override
    public void scram(ScramState state) {
        Maze maze = new Maze((Set<Node>)state.allNodes());

        greedy(state, maze);
    }

    /**
     * Returns whether or not there are coins left in the maze */
    public boolean coinsLeft(ScramState state){
        for(Node node: state.allNodes()) {
            if (node.getTile().coins() > 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the length of a List object which represents a path by adding up
     * the weights of the edges in the path*/
    public int pathLength(List<Edge> path) {
        int length = 0;
        for (Edge edge : path) {
            length += edge.length();
        }
        return length;
    }

    /**
     * Moves the Diver along the maze such that maximum coins are collected and returns to the
     * exit within the required steps.
     *
     * Every iteration of the while loop, the method finds the tile with the highest coin/step ratio
     * and that is feasible to go to, meaning that we can go to the tile and exit within remaining
     * steps.
     *
     * Using path checkers, if it is not possible to go to the exit within remaining steps, the loop
     * iteration will not run and immediately send the diver to the exit. */
    public void greedy(ScramState state, Maze maze){
        //Create a shortestPath object to find the shortest path from the current node to the exit
        ShortestPaths<Node,Edge> shortestPaths = new ShortestPaths(maze);

        //initialize the exit path and coin path to keep track of the shortest paths from the current node
        shortestPaths.singleSourceDistances(state.currentNode());
        List<Edge> exitPathCheck = shortestPaths.bestPath(state.exit());
        shortestPaths.singleSourceDistances(state.currentNode());
        List<Edge> coinPathCheck = shortestPaths.bestPath(state.currentNode());

        //create a set of all nodes in the maze that have coins on them
        Set<Node> coinNodes = new HashSet<>();
        for (Node node: state.allNodes()) {
            if (node.getTile().coins() > 0) {
                coinNodes.add(node);
            }
        }

        //loop while there are still coins left in the maze and the path length from the
        // current node to the coin added with the path length from the coin to the exit is less
        // than the steps left
        while(pathLength(exitPathCheck) + pathLength(coinPathCheck) < state.stepsToGo()&&coinsLeft(state)){
            int maxSteps = state.stepsToGo();

            //calculate the coins/step ratio for each node in the maze for a given node
            HashMap<Node, Double> coinsPerStep = new HashMap<>();
            for (Node node: coinNodes) {
                shortestPaths.singleSourceDistances(state.currentNode());
                List<Edge> tempPath = shortestPaths.bestPath(node);

                double totalCoins = 0;

                for (Edge edge : tempPath) {
                    totalCoins += edge.destination().getTile().coins();
                }

                coinsPerStep.put(node, totalCoins/pathLength(tempPath));
            }

            //get the node with the highest coins/step ratio
            Node bestNode = state.currentNode();
            double maxRatio = 0;
            for (Node node: coinNodes) {
                //find the paths from the current node to the coin and from the coin to the exit
                shortestPaths.singleSourceDistances(state.currentNode());
                List<Edge> pathCoin = shortestPaths.bestPath(node);
                shortestPaths.singleSourceDistances(node);
                List<Edge> pathExit = shortestPaths.bestPath(state.exit());

                //want to maintain the invariant that the path from current to bestNode to exit is
                //within the steps remaining
                if (coinsPerStep.get(node) > maxRatio && pathLength(pathExit) + pathLength(pathCoin)<= maxSteps){
                    maxRatio = coinsPerStep.get(node);
                    bestNode = node;
                    //update the best coin path
                    coinPathCheck = pathCoin;
                }
            }

            //break if the best node is the current node
            if (bestNode == state.currentNode()) {
                break;
            }

            //move to the node with the highest coins/step ratio if it is not null
            if (bestNode != null) {
                for (Edge edge : coinPathCheck) {
                    state.moveTo(edge.destination());
                }
            }
            //update the exit path
            shortestPaths.singleSourceDistances(state.currentNode());
            exitPathCheck = shortestPaths.bestPath(state.exit());

//            System.out.println("in loop");
//            System.out.println("exit path length: " + pathLength(exitPathCheck));
//            System.out.println("coin path length: " + pathLength(coinPathCheck));
        }
        //move diver to exit
        for (Edge exitEdge: exitPathCheck) {
            state.moveTo(exitEdge.destination());
        }
    }
}
