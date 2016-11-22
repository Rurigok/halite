import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Halite bot (https://halite.io)
 * Codename: Vanguard
 * 
 * Greedy border search with single-pass propagation.
 * 
 * @author Andrew Sanetra (rurigok)
 */
public class Vanguard {
	
	public static final String BOT_NAME = "Vanguard";
	public static final int NUM_PROPAGATIONS = 1;
	public static final double ENEMY_VALUE_WEIGHT = 1.1;
	public static final double VALUE_DECAY = 0.2;
	public static final int STRENGTH_THRESHOLD = 5;
	public static final int SEARCH_DISTANCE = 3;
	
	public static GameMap gameMap;
	public static int myID;
	public static PrintWriter logOut;
	
	public static Node[][] grid;
	public static int numMyTiles = 0;
	public static int propagations = 2;
	
	public static void logLine(String line) {
		logOut.println(line);
	}
	
    public static void main(String[] args) throws java.io.IOException {
    	
    	/*
    	 * Initialize the bot. After this point, we have 15 seconds to
    	 * pre-process and send our init command.
    	 */
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;
        grid = new Node[gameMap.width][gameMap.height];
        logOut = new PrintWriter(BOT_NAME + ".log.txt", "UTF-8");
        
        // Initialize our nodes with default targeting information
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
            	grid[x][y] = new Node();
            }
        }

        // Ready to play!
        Networking.sendInit(BOT_NAME);

        Site mySite;
        
        while (true) {
            ArrayList<Move> moves = new ArrayList<Move>();

            gameMap = Networking.getFrame();

            // Designate and acquire targets
            for (int i = 0; i < NUM_PROPAGATIONS; i++) {
	            for (int y = 0; y < gameMap.height; y++) {
	                for (int x = 0; x < gameMap.width; x++) {
	                	
	                	mySite = gameMap.getSite(new Location(x, y));

	                    if (mySite.owner == myID) {
	                        designateTarget(x, y, mySite);
	                    }
	                }
	            }
	            //logLine("========== end propagation ===========");
            }
            
            //dumpGrid();
            numMyTiles = 0;
            // Make moves
            for (int y = 0; y < gameMap.height; y++) {
                for (int x = 0; x < gameMap.width; x++) {
                	
                	mySite = gameMap.getSite(new Location(x, y));
                	
                    if (mySite.owner == myID) {
                    	numMyTiles++;
                        moves.add(new Move(new Location(x, y), getMove(x, y, mySite)));
                    }
                }
            }
            
            Networking.sendFrame(moves);
            propagations = (int) Math.max(2, Math.sqrt(numMyTiles));
        }
    }
    
    /**
     * Returns the resultant coordinates of moving in the given direction with
     * the given starting coordinates.
     * @param x Starting x
     * @param y Starting y
     * @param direction Direction to move in
     * @return A location object with the resultant location
     */
    public static Location getLocationInDirection(int x, int y, Direction direction) {
    	
    	switch (direction) {
    		default:
    			break;
    		case NORTH:
    			y--;
    			break;
    		case EAST:
    			x++;
    			break;
    		case SOUTH:
    			y++;
    			break;
    		case WEST:
    			x--;
    			break;
    	}
    	
    	// Wrap-around x and y values if necessary
    	x = wrapX(x);
    	y = wrapY(y);
    	
    	return new Location(x, y);
    }
    
    public static int wrapX(int x) {
    	if (x < 0) {
    		x += gameMap.width;
    	}
    	if (x >= gameMap.width) {
    		x -= gameMap.width;
    	}
    	return x;
    }
    
    public static int wrapY(int y) {
    	if (y < 0) {
    		y += gameMap.height;
    	}
    	if (y >= gameMap.height) {
    		y -= gameMap.height;
    	}
    	return y;
    }
    
    /**
     * Returns all locations + direction of all tiles within the
     * Manhattan distance of the center x, y.
     * 
	 * Example:
	 * If Manhattan distance is 2 and we are centered at 3,3, we should
	 * return the bracketed points.
	 * 
	 *  1,1    2,1   [3,1]   4,1    5,1
	 * 
	 *  1,2   [2,2]  [3,2]  [4,2]   5,2
	 * 
	 * [1,3]  [2,3]  !3,3!  [4,3]  [5,3]
	 * 
	 *  1,4   [2,4]  [3,4]  [4,4]   5,4
	 * 
	 *  1,5    2,5   [3,5]   4,5    5,5
	 *  
	 * The direction give need only point in the closest cardinal direction.
	 * If two cardinal directions are equidistant, choose randomly.
     * 
     * @param x Center x
     * @param y Center y
     * @param manhattanDistance size of von Neumann neighborhood to find
     * @return A list of locations and associated directions
     */
    public static ArrayList<Path> searchNeighborhood(int x, int y, int manhattanDistance) {
    	ArrayList<Path> paths = new ArrayList<>();
    	
    	// TODO; this only returns manhattanDistance = 1
    	Path p;
    	for (Direction d : Direction.CARDINALS) {
    		p = new Path(getLocationInDirection(x, y, d), d);
    		paths.add(p);
    	}
    	
//    	Direction d = Direction.NORTH;
//    	for (int dy = -manhattanDistance; dy <= manhattanDistance; dy++) {
//            for (int dx = -manhattanDistance; dx <= manhattanDistance; dx++) {
//            	p = new Path(new Location(wrapX(x + dx), wrapY(y + dy)), Direction.NORTH);
//            }
//    	}
    	
    	return paths;
    }
    
    /**
     * Designate a target for a node
     * @param x X of the node
     * @param y Y of the node
     */
    public static void designateTarget(int x, int y, Site mySite) {
    	
    	Node myNode = grid[x][y];
    	
    	Site targetSite;
    	Location targetLocation;
    	double targetValue;
    	
    	Node alliedNode;
    	
    	//logLine("====== Node at: " + x + ", " + y);
    	//logLine(myNode.toString());
    	
    	/*
    	 * Search the surrounding four sites for high value targets. This
    	 * can be in the form of a direct neutral/enemy target or helping
    	 * another node attack some other target.
    	 */
    	//for (Direction d : Direction.CARDINALS) {
    	for (Path p : searchNeighborhood(x, y, SEARCH_DISTANCE)) {
    		
    		targetLocation = p.location;
    		targetSite = gameMap.getSite(targetLocation);
    		
    		if (targetSite.owner == 0) { // Neutral site
    			
    			targetValue = (double) targetSite.production / Math.max(targetSite.strength, 1);
    			
    			/*
    			 * myNode should always contain the highest value targeting
    			 * information. If we find a higher value, update it.
    			 */
    			if (targetValue > myNode.targetValue) {
    				myNode.targetValue = targetValue;
    				myNode.targetDirection = p.direction;
    				myNode.targetNeed = targetSite.strength;
    			}
    			
    		} else if (targetSite.owner != myID) { // Enemy site
    			
    			//targetValue = (double) targetSite.production / Math.max(targetSite.strength, 1) * ENEMY_VALUE_WEIGHT;
    			targetValue = Math.max(targetSite.strength, 1);
    			
    			if (targetValue > myNode.targetValue) {
    				myNode.targetValue = targetValue;
    				myNode.targetDirection = p.direction;
    				myNode.targetNeed = targetSite.strength;
    			}
    			
    		} else { // Allied site

    			alliedNode = grid[targetLocation.x][targetLocation.y];
    			targetValue = (double) alliedNode.targetValue * VALUE_DECAY;
    			
    			if (targetValue > myNode.targetValue) {
    				myNode.targetValue = targetValue;
    				myNode.targetDirection = p.direction;
    				myNode.targetNeed = alliedNode.targetNeed - targetSite.strength;
    			}
    			
    		}
    		
    	}
    	
    	//logLine("Site strength: " + mySite.strength);
    	//logLine("Target site strength: " + gameMap.getSite(getLocationInDirection(x, y, myNode.targetDirection)).strength);
    	//logLine(myNode.toString());
    	
    }
    
    public static Direction getMove(int x, int y, Site mySite) {
    	
    	Node myNode = grid[x][y];
    	Direction move;
    	
    	if (mySite.strength >= myNode.targetNeed && mySite.strength >= STRENGTH_THRESHOLD) {
    		//logLine("Move command issued: " + myNode.targetDirection);
    		
    		move = myNode.targetDirection;
    		
    		myNode.clearTarget();
    		return move;
    	}
    	
    	return Direction.STILL;
    }
    
    public static void dumpGrid() {
    	StringBuilder line = new StringBuilder();
    	for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
            	if (gameMap.getSite(new Location(x, y)).owner == myID) {
	            	switch (grid[x][y].targetDirection) {
					case EAST:
						line.append(">");
						break;
					case NORTH:
						line.append("^");
						break;
					case SOUTH:
						line.append("v");
						break;
					case STILL:
						line.append(".");
						break;
					case WEST:
						line.append("<");
						break;
	            	}
            	} else {
            		line.append("x");
            	}
            	line.append(" ");
            }
            logLine(line.toString());
            line = new StringBuilder();
        }
    	logLine("==========================================================");
    }

}
