package AIFinalProject;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
//import org.graphstream.algorithm.*;
import org.graphstream.algorithm.generator.*;

//import java.awt.Point;
//import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
//import java.util.Set;
import java.util.Scanner;

public class RobotSearch {
	
    //graph is set up with all variables via user input and search is initiated
	public static void main(String args[]) {
		Graph graph = new SingleGraph("Environment Graph"); //graph created
		System.setProperty("org.graphstream.ui", "javafx");
		Generator gen = new GridGenerator();
		
		graph.setAttribute("ui.stylesheet", styleSheet);
		gen.addSink(graph);
		gen.begin();
		
		//get input for specifying graph size
		System.out.println("Please specify the size of the graph to be traversed.");
		System.out.print("Enter the number of rows and number of columns (one number): ");
		Scanner in = new Scanner(System.in);
		int maxBorder = getIntegerInput(in) - 1;
		
		//int maxBorder = 10;
		
		for(int i = 0; i< maxBorder; i++) {
			gen.nextEvents();
		}

		gen.end();

		
		
		for (Node node : graph) {
            node.setAttribute("ui.label", node.getId());
        }
		
		
		//Set up graph with base station, robots, and obstacles
		
		graph.getNode("0_0").setAttribute("ui.class", "base"); //set base station
		
		//get robot amount and place them
		System.out.print("Please specify the number of robots to traverse the graph: ");
		int numOfRobots = getIntegerInput(in);
		/*graph.getNode("0_1").setAttribute("ui.class", "robot");
		graph.getNode("1_0").setAttribute("ui.class", "robot");*/
		//graph.getNode("1_1").setAttribute("ui.class", "robot");
		Node [] robots = placeRobots(numOfRobots, graph);
		
		//get obstacle amount and place them
		System.out.println("How many obstacles should be placed in your graph?");
		System.out.println("Based on the selected graph size, a suggestion might be " + maxBorder + " obstacles as a healthy amount.");
		System.out.print("Please select number obstacles: ");
		int numOfObst = getIntegerInput(in);
		generateObstacles(numOfObst, graph);
		/*
		graph.getNode("0_6").setAttribute("ui.class", "obstacle");
		graph.getNode("2_3").setAttribute("ui.class", "obstacle");
		*/
		
		//display graph
		// Nodes already have a position, hence false parameter
		graph.display(false);
		sleep();
		
		//Node[] robots = new Node[] {graph.getNode("1_0"),graph.getNode("0_1")/*, graph.getNode("1_1")*/};
        search(robots, graph);
    }
    
    public static String[][] calcMaxConfig(Node[] robots, Graph graph) {
    	final int impossible = -7; //impossible move and its corresponding utility value
		final int lossOfComm = -7; //utility value associated with a move in which communication is lost between robots
		final int possibleMoves = 5; //number of possible moves by a robot: up, down, left, right, none
		int utility = 0;
		int maxUtility = (int)Double.NEGATIVE_INFINITY;
    	int k = (int)Math.pow(robots.length, possibleMoves); //number of possible configurations: (# of robots)^(# of possible moves)
    	String[][] config = new String [robots.length][2]; //configuration array of n robots initialized
		String[][][] pop =  new String [k][robots.length][2]; //population array of k configurations
		String[][] configMax = new String [robots.length][2]; //maximum configuration to be stored and utilized for choosing best collective movement of robots
		
		//get population array
		pop = getPopulation(robots, graph, k);
		
		//coalesce configurations of movements with current robot positions to test legality of moves and thus calculate utility
		for (int m = 0; m < k; m++) {
			config = pop[m];
			for (int n = 0; n < robots.length; n++) {
				String currNode = robots[n].getId();
        		String[] coords = currNode.split("_");
        		System.out.println("Coords of robot " + n + " of configuration " + m + " before move: (" + coords[0] + ", " + coords[1] + ")");
        		System.out.println("Configuration " + m + ": " + Arrays.deepToString(config));
        		int nextX = (Integer.parseInt(coords[0]) + Integer.parseInt(config[n][0]));
        		int nextY = (Integer.parseInt(coords[1]) + Integer.parseInt(config[n][1]));
        		String robotMoveStr = (nextX + "_" + nextY);
        		System.out.println("Next node of robot position after move would be: " + robotMoveStr);
        		////sleep();
        		Node robotMove = graph.getNode(robotMoveStr);
        		
        		//Possible utility calculations are fancied:
        		if (nextX < 0 || nextX >= (Math.sqrt(graph.getNodeCount())) || nextY < 0 || nextY >= (Math.sqrt(graph.getNodeCount()))) {
        			System.err.println("This move is out of bounds and will have add a utility of " + impossible + " to configuration " + m);
        			utility += impossible;
        		}
        		else if (robotMove.getAttribute("ui.class") == "obstacle" || robotMove.getAttribute("ui.class") == "base") {
        			System.err.println("This space is occupied by an obstacle or base station. Therefore, a utility of " + impossible + " will be added to configuration " + m);
        			utility += impossible;
        		}
        		else if (!inComm(robotMove, robots)) {
        			System.err.println("This move will result in a loss of communication. A utility of " + lossOfComm + " will be added to configuration " + m);
        			utility += lossOfComm;
        		}
        		else if (robotMove.getAttribute("ui.class") == "robot") {
        			System.err.println("This space is occupied by another robot. Let's check if any robots are not moving in configuration " + m);
        			for (int p = 0; p < robots.length; p++) {
        				if (config[p][0] == "0" && config[p][1] == "0") {
        					System.out.println("Movement for robot " + p + " in configuration " + m + ": " + config[p]);
        					System.out.println("It seems robot " + p + " will not be moving in configuration " + m + ". Let's check if this is our robot we're concerned with bumping into.");
        					if (robots[p].getId() == robotMove.getId()) {
        						System.out.println(robots[p].getId() + " and " + robotMove.getId() + " are one in the same. Yep this is an ol' fashioned robot bumparoo.");
        						System.err.println("Let's add " + impossible + " to the utility of configuration " + m);
        						utility += impossible;
        					}
        					else {
        						System.out.println("Nope. We good.");
        						if (p == (robots.length - 1)) {
        							System.out.println("That was the last robot to check. We can move there as it will be visited cell after the robot occupying the cell moves. The utility added for moving to this previously occupied cell will be the manhattan distance to the nearest frontier cell multiplied by negative 1.");
        							System.out.println("This utility will be " + manhattan(robotMove));
        							utility += manhattan(robotMove);
        						}
        						else {
        							System.out.println("Let's keep checking the rest of the robots. There are more.");
        						}
        					}
        				}
        			}
        		}
        		else if (robotMove.getAttribute("ui.class") == "frontier") {
        			System.out.println("Here is a frontier node. The utility added to configuration " + m + " for moving robot " + n + " here will be 0");
        			utility += 0;
        		}
        		else if (robotMove.getAttribute("ui.class") == "visited") {
        			System.out.println("Visited node. The utility for configuration " + m + " for moving robot " + n + " here will be " + manhattan(robotMove));
        			utility += manhattan(robotMove);
        		}
        		
			}
			//update max utility
			if (utility > maxUtility) {
    			maxUtility = utility;
    			configMax = config;
    		}
			utility = 0; //reset utility for the next configuration 
		}
		//print out calculated movements
		System.out.println("The maximum configuration found consists of:");
		for (int q = 0; q < robots.length; q++) {
			System.out.println("A move of " + configMax[q][0] + " to the right and a move of " + configMax[q][1] + " cells upwards for robot " + robots[q].getId());
		}
		//return maximum calculated configuration
		return configMax;
    }
    
    //draws frontier surrounding a robot, called upon initial graph setup and after a robot has moved to a new cell
    public static void drawFrontier(Node robot) {
    	Iterator<? extends Node> front = robot.getBreadthFirstIterator();
    	System.out.println("Drawing Frontier.");
    	for (int i = 0; i <=4; i++) {
    		Node next = front.next();
    	    if (next.getAttribute("ui.class") == null) {
    	    	next.setAttribute("ui.class", "frontier");
    	        System.out.println("Setting unexplored node " + next + " to frontier");
    	    }
    	    //sleep();
    	}
    }
    
    //finds Euclidean distance between two nodes (conveniently finds distance without being given coordinates)
    public static double euclidean(Node one, Node two) {
    	int[] coordsOne = getCoords(one);
    	int[] coordsTwo = getCoords(two);
    	int x1 = coordsOne[0];
    	int x2 = coordsTwo[0];
    	int y1 = coordsOne[1];
    	int y2 = coordsTwo[1];
    	return Math.floor((Math.sqrt(Math.pow((x2-x1), 2) + Math.pow((y2 - y1), 2)))*100)/100;
    }
    
    //finds Euclidean distance between two nodes when given coordinates of each node (truncates value to two decimal places)
    public static double euclideanDist(int x1, int y1, int x2, int y2) {
    	return Math.floor((Math.sqrt(Math.pow((x2-x1), 2) + Math.pow((y2 - y1), 2)))*100)/100;
    }
    
    //performs actual move for a robot once move is deemed legal and maximum configuration is found
    public static void executeMove(Node from, Node to) {
    	//set robot's previous position to visited and position of next move to robot
		System.out.println("Setting previous robot position to visited");
		from.setAttribute("ui.class", "visited");
		System.out.println("Setting next robot position based on calculated move");
		to.setAttribute("ui.class", "robot");
		drawFrontier(to);
    }
    
    //finds shortest Euclidean distance to a desired goal node from amongst an array of move options
    public static Node findShortestEuclidean (Node convention, Node [] options) {
    	Node closest = options[0]; // by default, maybe shouldn’t do default
    	System.out.println("Analyzing moves to acheive shortest Euclidean distance from robot " + convention.getId());
    	for (int i = 0; i < options.length; i++) {
    		if ((euclidean(convention, options[i]) < euclidean(convention, closest)) && options[i].getAttribute("ui.class") != "obstacle" && options[i].getAttribute("ui.class") != "robot" && options[i].getAttribute("ui.class") != "base") {
    			closest = options[i];
    		}
    	}
    	System.out.println("A movement to cell " + closest.getId() + " will acheive shortest Euclidean distance from robot " + convention.getId() + " and reduce distance between robots for communication purposes");
    	return closest;
    }
    
    //generates obstacles randomly within the graph according to specified amount of obstacles to be generated
    public static void generateObstacles(int numOfObst, Graph graph) {
    	if (numOfObst == 0) {
    		return;
    	}
    	int originalNumOfObst = numOfObst; //placeholder value for when numOfObst value. This is because numOfObst is increased in for loop to retry placement of obstacles that are originally chosen to be placed in illegal positions
    	int maxIndex = (int)Math.sqrt(graph.getNodeCount());
    	String nodeString;
    	for (int i = 0; i < numOfObst; i++) {
    		nodeString = rand(maxIndex) + "_" + rand(maxIndex);
    		Node node = graph.getNode(nodeString);
    		if (node.getAttribute("ui.class") == "robot" || node.getAttribute("ui.class") == "base" || node.getId() == "0_0") {
    			System.out.println("The node at " + getCoordsString(node) + " is either a robot or base station and cannot be set as an obstacle.");
    			numOfObst++; //increase numofObst because no obstacle was placed here
    		}
    		else {
    			Node obstacle = graph.getNode(nodeString);
    			System.out.println("Setting node at " + getCoordsString(obstacle) + " to be an obstacle (randomly selected location)");
    			obstacle.setAttribute("ui.class", "obstacle");
    			System.out.println("There are " + originalNumOfObst + " total obstacles selected to be placed.");
    	
    		}
    	}
    	sleep();
    	return;
    }
    
    //gets the coordinates of a specified node
    public static int[] getCoords (Node node) {
    	String nodeStr = node.getId();
		String[] coords = nodeStr.split("_");
		System.out.println("Coords of node: (" + coords[0] + ", " + coords[1] + ")");
		int x = (Integer.parseInt(coords[0]));
		int y = (Integer.parseInt(coords[1]));
		int [] coordsInt = {x,y};
		return coordsInt;
    }
    
    //converts coordinates to lovely string format
    public static String getCoordsString (Node node) {
    	String nodeStr = node.getId();
		String[] coords = nodeStr.split("_");
		return "(" + coords[0] + ", " + coords[1] + ")";
    }
    
    //gets integer input from user
    //will keep prompting user for integer until integer is entered, then returns entered integer
    public static int getIntegerInput(Scanner in) {
    	int input = -1;
    	do {
			try {
					input = Integer.parseInt(in.nextLine());
				}
			catch (NumberFormatException e) {
				System.err.print("Please enter an applicable integer: ");
			}
			catch (NoSuchElementException e) {
				input = Integer.parseInt(in.nextLine());
			}
		} while (input == -1);
    	
    	return input;
    }
    
    //calculates configurations by calling randMove and store configurations in population array
    public static String[][][] getPopulation(Node[] robots, Graph graph, int k) {
    	String[][] config = new String [robots.length][2]; //configuration array of n robots initialized
		String[][][] pop =  new String [k][robots.length][2]; //population array of k configurations
    	for (int i = 0; i < k; i++) {
			for (int j = 0; j < robots.length; j++) {
				//drawFrontier(robots[j]);
				int[] movement = randMove(robots[j]);
        		System.out.println("Configuration " + i + ", Robot " + j + " calculated move: " + movement[0] + " units to the right and " + movement[1] + " units upward");
        		config[j][0] = Integer.toString(movement[0]);
        		config[j][1] = Integer.toString(movement[1]);
      
			}
			System.out.println("Population array index: " + i);
			pop[i] = config;
			System.out.println("Configuration array: " + Arrays.deepToString(config));
			//System.out.println("Population array of configurations so far: " + Arrays.deepToString(pop));
		}
		System.out.println("Population array of configurations: " + Arrays.deepToString(pop));
		return pop;
    }
    
    //checks if a given robot is within communication maximum communication range of all other robots
    public static boolean inComm(Node robot, Node[] robots) {
    	int maxRange = (int)(Math.sqrt(robot.getGraph().getNodeCount())/2);
		System.out.println("Coords of current robot node before move: " + getCoordsString(robot));
		int[] coords = getCoords(robot);
		int x = (coords[0]);
		int y = (coords[1]);
    	Iterator<? extends Node> findFront = robot.getBreadthFirstIterator();
    	
    	for (int i=0; i < robots.length; i++) {
    		if (robot.getId() != robots[i].getId()) {
    			Node second = robots[i];
    			int [] secCoords = getCoords(second);
    			/*String secondStr = second.getId();
    			String[] secondCoords = secondStr.split("_");
    			int secX = (Integer.parseInt(secondCoords[0]));
    			int secY = (Integer.parseInt(secondCoords[1]));*/
    			int secX = secCoords[0];
    			int secY = secCoords[1];
    			System.out.println("Euclidean distance between robot " + robot.getId() + " and " + second.getId() + ":");
    			if (euclideanDist(x, y, secX, secY) > maxRange) {
    				System.err.println(euclideanDist(x, y, secX, secY) + " is greater than the max range of " + maxRange);
    				return false;
    			}
    			else {
    				System.out.println(euclideanDist(x, y, secX, secY) + ", which is within the max range of " + maxRange);
    			}
    		}
    		
    	}
    	System.out.println("All robots are within range of eachother.");
    	return true; //mandatory return statement if frontier is never found, in which case the grzph should be completely traversed
    }
    
    //calculates manhattan distance to nearest frontier cell and returns the value to be added to utility of a concerned configuration
    public static int manhattan(Node robot) {
    	String coordStr = robot.getId();
		String[] coords = coordStr.split("_");
		System.out.println("Coords of current robot node before move: (" + coords[0] + ", " + coords[1] + ")");
		int x = (Integer.parseInt(coords[0]));
		int y = (Integer.parseInt(coords[1]));
    	Iterator<? extends Node> findFront = robot.getBreadthFirstIterator();
    	Node current = findFront.next();
    	while (findFront.hasNext()) {
    		Node next = findFront.next();
    		if (next.getAttribute("ui.class") == "frontier") {
    			String frontStr = next.getId();
    			String[] frontCoords = frontStr.split("_");
    			System.out.println("Coords of nearest frontier node: (" + frontCoords[0] + ", " + frontCoords[1] + ")");
    			int frontX = (Integer.parseInt(frontCoords[0]));
    			int frontY = (Integer.parseInt(frontCoords[1]));
    			System.out.println("Returning " + -(Math.abs(frontX - x) + Math.abs(frontY - y)) + " for utility calcuated by Manhattan algorithm.");
    			return -(Math.abs(frontX - x) + Math.abs(frontY - y));
    		}
    	}
    	System.err.println("Frontier never found. In this case, graph should be completely traversed.");
    	return 0; //mandatory return statement if frontier is never found, in which case the grzph should be completely traversed
    }
    
    //performs move for robots, calls calcMaxConfig to determine optimal moves before doing so
    public static Node[] move(Node[] robots, Graph graph) {
    	Node[] bumpingRobots = new Node[robots.length];
		String[][] configMax = new String [robots.length][2];
		configMax = calcMaxConfig(robots, graph);
		////sleep();
		
		Node[] robotMoves = new Node [robots.length];
		//commence calculated movements 
		for (int r = 0; r < robots.length; r++) {
			drawFrontier(robots[r]);
    		System.out.println("Coords of current robot node before move: " + getCoordsString(robots[r]));
    		int [] coords = getCoords(robots[r]);
    		int nextX = (coords[0] + Integer.parseInt(configMax[r][0]));
    		int nextY = (coords[1] + Integer.parseInt(configMax[r][1]));
    		String robotMoveStr = (nextX + "_" + nextY);
    		System.out.println("Next node of robot position after move will be: " + robotMoveStr);
    		
    		//move is stored, then executed if possible
    		robotMoves[r] = graph.getNode(robotMoveStr);
    		
    		//unused call to function r
    		/*if (!inComm(robots[r], robots)) {
    			robotMoves[r] = moveBackWithinRange(robots[r], robots);
    		}
    		
    		else */if (outOfBounds(robotMoves[r], graph)) {
    			System.err.println("This is an out-of-bounds move. Robot will not move.");
    			robotMoves[r] = robots[r];
    		}
    		else if (robotMoves[r].getAttribute("ui.class") == "obstacle" || robotMoves[r].getAttribute("ui.class") == "base") {
    			System.err.println("This is an impossible move. Robot will not move.");
    			robotMoves[r] = robots[r];
    		}
    		else if (!inComm(robotMoves[r], robots)) {
    			System.err.println("This move will result in a loss of communication. Therefore robot will not move.");
    			robotMoves[r] = robots[r];
    		}
    		else if (robotMoves[r].getAttribute("ui.class") == "robot") {
    			System.err.println("Need to perform the move of robot in this position before the concerned robot moves");
    			System.out.println("Let's keep note of this concerned robot and perform its move later if the robot blocking it has moved");
    			bumpingRobots[r] = robotMoves[r];
    			/*
    			for (int s = 0; s < robots.length; s++) {
    				if (robots[r].getId() == robotMoves[r].getId()) {
    					
    				}
    			}*/
    			sleep();
    		}
    		else {
    			executeMove(robots[r], robotMoves[r]);
    		}
			
		}
		
		//Need to perform moves for robots that were blocked by other robots if they are legal moves now
		for (int s = 0; s < robots.length; s++) {
			if (bumpingRobots[s] != null) {
				if (bumpingRobots[s].getAttribute("ui.class") != "robot") {
					executeMove(robots[s], robotMoves[s]);
				}
				else {
					robotMoves[s] = robots[s];
				}
			}
		}
		
		//sleep();
		return robotMoves;
		
    }
    
    //The function is not used due to algorithm's capability to coagulate robots via utility calculations
    
    //moves robots back within range if they ever move out of range of eachother
    /*public static Node moveBackWithinRange (Node robot, Node[] robots) {
    	System.err.println("moving robot " + robot.getId() + " back in range.");
    	Node move = robot; //set default move as nothing to ensure null isn't returned
    	while (!inComm(robot, robots)) {
    		Iterator<? extends Node> iterator = robot.getBreadthFirstIterator();
    		Node curr = iterator.next();
    		Node[] moves = new Node [4];
    		for (int i = 0;i < 4; i++) {
    			moves[i] = iterator.next();
    		}
    		//for loop is incorporated in case robot parameter passed to this function is robot at index 0 of robots array, which is supposed to be convention point
    		for (int j = 0; j < robots.length; j++) {
    			if (curr.getId() != robots[j].getId()) {
    				move = findShortestEuclidean(robots[j], moves);
    				System.out.println("Moving robot at " + robot.getId() + " to " + move.getId() + " to get closer to robot " + j + " located at cell " + robots[j].getId());
    				executeMove(robot, move);
    				break;
    			}
    		}
    	}
    	return move; //return node where robot moved to so robotMoves array can be updated accordingly
    }
    */
    
    //checks if a node is out of bounds of graph
    public static boolean outOfBounds(Node node, Graph graph) {
    	try {
    		String moveStr = node.getId();
    		String[] indecesStr = moveStr.split("_");
    		int[] indeces = {Integer.parseInt(indecesStr[0]), Integer.parseInt(indecesStr[1]) };
    		if(indeces[0] < 0 || indeces[0] >= (Math.sqrt(graph.getNodeCount())) || indeces[1] < 0 || indeces[1] >= (Math.sqrt(graph.getNodeCount()))) {
    			return true;
    		}
    		else {
    			return false;
    		}
    	}
    	catch (NullPointerException e){
    		System.err.println("Node cannot be moved to, highly likely due to being out-of-bounds.");
    		return true;
    	}
		
    }
    
    //places robots in the graph according to specified number of robots 
    //all robots are placed as close as possible to the base station initially, which is set to be at node (0,0)
    //function returns Node array of robots placed
    public static Node[] placeRobots (int numOfRobots, Graph graph) {
    	Node base = graph.getNode("0_0");
    	Iterator<? extends Node> it = base.getBreadthFirstIterator();
    	it.next(); //next is called once to account for base station node
    	Node [] robots = new Node[numOfRobots];
    	for (int i = 0; i < numOfRobots; i++) {
    		robots[i] = it.next();
    		System.out.println("A robot will begin at node with coordinates " + getCoordsString(robots[i]));
    		robots[i].setAttribute("ui.class", "robot");
    	}
    	
    	return robots;
    }
    
    //returns randomly selected move from the options of up, down, left, right, or none
    public static int[] randMove(Node robot) {
    	int[][] M = { {0,1}, {1,0}, {0,-1}, {-1,0}, {0,0} };
    	int randMove = (int)(Math.random() * (M.length)); //add plus 1 in equation because Math.random() does is not upper inclusive in range from 0 to 1?
    	System.out.println("Random number selected for index of movements: " + randMove);
    	//sleep();
    	return M[randMove];
    }
    
    //returns random numbered index within specified length array
    public static int rand (int length) {
    	return (int)(Math.random() * length);
    }
    
    //commences robot search of the provided graph
    public static void search(Node[] robots, Graph graph) {
        /*Iterator<? extends Node> k1 = robots[0].getBreadthFirstIterator();
        */
        
        //draw initial frontier
        for (Node robot: robots) {
    		drawFrontier(robot);
    	}
        sleep();
        int T = (int)Math.pow(graph.getNodeCount(), 2); //number of time steps to perform
        int t = 0;
        while (t < T && !searchComplete(graph)) {
        	for (int i = 0; i < robots.length; i++) {
        		System.out.println("Calling move function");
        		robots = move(robots, graph);
        		//sleep();
        		t++;
        	}
        if (searchComplete(graph)) {
    			System.err.println("The graph has been traversed! And with " + (T - t) + " steps to spare.");
    	}	
        else {
        	System.err.println("Search complete. Although the graph has not been completely traversed, the specified number of " + T + " time steps have elapsed.");
        }
        	
        }
        
    }
    
    //calculates if graph if completely traversed
    public static boolean searchComplete (Graph graph) {
    	for (Node n: graph) {
    		if (n.getAttribute("ui.class") == null || n.getAttribute("ui.class") == "frontier") {
    			return false;
    		}
    	}
    	return true;
    }
    
    ////sleep function to provide delay between certain functions
    protected static void sleep() {
        try { Thread.sleep(100); } catch (Exception e) {}
    }

    //style sheet for types of nodes (unvisited, visited, frontier, base station, robot, and obstacle)
    protected static String styleSheet =
    		"node {" +
    				"fill-color: black;" +
    				"}" +
    		"node.robot {" +
    	    	    "fill-color: blue;" +
    	    	    "size: 15px;" +
    	    	    "}" +
    		"node.visited {" +
    	    		"fill-color: green;" +
    	    		"size: 10px;" +
    	    		"}" +
    	    "node.frontier {" +
    	    	    "fill-color: yellow;" +
    	    	    "size: 10px;" +
    	    	    "}" +
    	    "node.obstacle {" +
    	    	    "	fill-color: red;" +
    	    	    "   size: 15px;" +
    	    	    "}" +
    		"node.base {" +
    				"fill-mode: gradient-radial;" + 
    				"fill-color: purple, white;" +
    				"size: 30px;" + 
        			/*"shape: box" +*/
        			"}";
}


