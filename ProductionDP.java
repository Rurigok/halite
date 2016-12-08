import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;


public class ProductionDP {
	
	//public static PrintWriter writer;
	
	public static GameMap gameMap;
	public static ArrayList<HashMap<ArrayList<Integer>,Integer[][]> > optMoves = new ArrayList<HashMap<ArrayList<Integer>,Integer[][]> >();
	public static ArrayList<HashMap<ArrayList<Integer>,Direction[][]> > optDirs = new ArrayList<HashMap<ArrayList<Integer>,Direction[][]> >();
	public static ArrayList<HashMap<ArrayList<Integer>,Integer> > optRounds = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
	public static ArrayList<HashMap<ArrayList<Integer>,Integer> > optSquareToTake = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
	public static ArrayList<HashMap<ArrayList<Integer>,Integer> > optProds = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
	public static ArrayList<HashMap<ArrayList<Integer>,Integer[][]> > optExcessStr = new ArrayList<HashMap<ArrayList<Integer>,Integer[][]> >();
	
	public static HashMap<Integer,ArrayList<ArrayList<Integer>> > solsByRound = new HashMap<Integer,ArrayList<ArrayList<Integer>> >(); 
	
	public static HashMap<Integer,Integer> optProdPerRound = new HashMap<Integer,Integer>();
	public static HashMap<Integer,ArrayList<Integer> > optSolPerRound = new HashMap<Integer,ArrayList<Integer> >();
	
	public static int startingOffset;
	public static long startTime;
	//public static int maxRound;
	//public static int oldMaxRound;
	public static int roundToCompute;
	public static int endOfGame;
	public static int lastRoundComputed;
	
	public static Boolean gameCaughtUp;
	
	
	public static void main(String[] args) throws java.io.IOException {
	    	
	    	//writer = new PrintWriter("haliteLog.xls");
	    	
	    
	        InitPackage iPackage = Networking.getInit();
	        startTime = System.currentTimeMillis();
	        int myID = iPackage.myID;
	        gameMap = iPackage.map;
	        endOfGame = (int) (10*Math.sqrt(gameMap.width*gameMap.height));
	        gameCaughtUp = false;
	        
	        //Preprocess the board here.
	        baseCase(myID,300);
	        computeOpt(myID,14,endOfGame);
	        ArrayList<ArrayList<Move> > optimalMoves = getOptimalMoves();
	        int currentMove = 0;	 
	        
	        
	        int[][] distToBorder = new int[gameMap.width][gameMap.height];
		    int[][] strengthNeeded = new int[gameMap.width][gameMap.height];
		    int MaxDist = 1;
		    int round = 0;
		    
		    int switchToKill = 400;
	        
	        
	        Networking.sendInit("ProductionDP");
	        
	        
	
	        while(true) {
	            
	        	//ArrayList<Move> moves = new ArrayList<Move>();
	
	            gameMap = Networking.getFrame();
	            startTime = System.currentTimeMillis();
	            
	            /*for(int y=0; y<gameMap.height; y++){
	            	for(int x=0; x<gameMap.width; x++){
	            		
	            		if(gameMap.getSite(new Location(x,y)).owner == myID){
	            			
	            			if(gameMap.getSite(new Location(x,y)).strength == 0){
	            				moves.add(new Move(new Location(x,y),Direction.STILL));
	            			}
	            			else{
	            				moves.add(new Move(new Location(x,y),Direction.SOUTH));
		            			moves.add(new Move(new Location(x,y),Direction.NORTH));
		            			
	            			}
	            		}
	            	}
	            }
	            
	            Networking.sendFrame(moves);*/
	            
	           /* writer.println("Actual Str.  Round " + round);writer.flush();
				
				for(int y=0; y<gameMap.height; y++){
					for(int x=0; x<gameMap.width; x++){
						Site thisSite = gameMap.getSite(new Location(x,y));
						
						if(thisSite.owner != myID){
							writer.print(-1*thisSite.strength);
							writer.print("\t");
						}
						else{
							writer.print(thisSite.strength);
							writer.print("\t");
						}
					}
					
					writer.println();
				}
				
				writer.flush();*/
	            
	            if(round==0){
	            	
	            	//writer.println("Size: " + optimalMoves.size()); writer.flush();
	            	round++;
	            	Networking.sendFrame(optimalMoves.get(0));
	            	optimalMoves.remove(0);
	            }
	            else{
	            
		            round++;
		            
		            if(gameCaughtUp){
		            	
		            	Networking.sendFrame(greedy(myID, distToBorder, strengthNeeded));
		            }
		            else{
		            	
			            if(optimalMoves.size() == 0){
			            	
			            	//writer.println("We used all precomputed moves.  Now need to get updated moves."); writer.flush();
			            	optimalMoves = getOptimalMoves();
			            	
			            }
			            
			            //writer.println("Size: " + optimalMoves.size()); writer.flush();
			            computeOpt(myID,1,endOfGame);//maxRound+1);
			            
			            if(optimalMoves.size() > 0){
			          
			            	//writer.println("Sending in " + optimalMoves.get(0).size() + " moves after " + (System.currentTimeMillis()-startTime) + "ms."); writer.flush();
			            	Networking.sendFrame(optimalMoves.get(0));
			            	optimalMoves.remove(0);
			            }
			            else{
			            	//writer.println("Using kill mode"); writer.flush();
			            	Networking.sendFrame(greedy(myID, distToBorder, strengthNeeded));
			            }
		            }
		            			
		            			
		            currentMove++;
	            }
	            
	            
	        }
	        
	            
	        
	        
	}
	
	public static void baseCase(int myID, int desiredRound){
		
		//writer.println("In baseCase"); writer.flush();
		
		roundToCompute = 0;
		lastRoundComputed = -1;
		
		ArrayList<Integer> baseSolution = new ArrayList<Integer>();
		Integer[][] baseMove = new Integer[gameMap.width][gameMap.height];
		Integer[][] baseExcessStr = new Integer[gameMap.width][gameMap.height];
		Direction[][] baseDirection = new Direction[gameMap.width][gameMap.height];
		//int baseRound = 1;
		int baseProd = 0;
		
		for(int y=0; y<gameMap.height; y++){			
			for(int x=0; x<gameMap.width; x++){
				
				baseDirection[x][y] = Direction.STILL;
				
				Site site = gameMap.getSite(new Location(x,y));
				if(site.owner == myID){
					
					baseSolution.add(y*gameMap.width + x);
					baseMove[x][y] = -1;
					baseProd += site.production;
					baseExcessStr[x][y] = site.strength;
					//writer.println("Starting str: " + site.strength);
				}
				else{
					baseMove[x][y] = -2;
					baseExcessStr[x][y] = 0;
				}
				
			}
		}
		
		ArrayList<ArrayList<Integer>> baseSolSet = new ArrayList<ArrayList<Integer>>();
		baseSolSet.add(baseSolution);
		
		
		for(int i=0; i<=desiredRound; i++){
			optProdPerRound.put(i, baseProd);
			optSolPerRound.put(i, baseSolution);
			
			if(i==0)
				solsByRound.put(0, baseSolSet);
			else
				solsByRound.put(i, new ArrayList<ArrayList<Integer> >());
		}
		
		
		startingOffset = baseSolution.size() - 1;
		
		
		/*writer.print("Base Solution: ");
		for(int z=0; z<baseSolution.size(); z++) 
			writer.print("(" + baseSolution.get(z)%gameMap.width + ", " + baseSolution.get(z)/gameMap.width + ")   ");
		
		writer.println(); writer.println(); writer.flush();*/
		
		/*writer.println("Base Move: ");
		for(int y=0; y<gameMap.height; y++){			
			for(int x=0; x<gameMap.width; x++){
				
				writer.print(baseMove[x][y]);
				writer.print('\t');
			}
			
			writer.println();
		}
		
		writer.println(); writer.println();
		
		writer.flush();*/
		
		HashMap<ArrayList<Integer>,Integer[][]> hashBaseMove = new HashMap<ArrayList<Integer>,Integer[][]>();
		hashBaseMove.put(baseSolution, baseMove);
		HashMap<ArrayList<Integer>,Direction[][]> hashBaseDirection = new HashMap<ArrayList<Integer>,Direction[][]>();
		hashBaseDirection.put(baseSolution, baseDirection);
		HashMap<ArrayList<Integer>,Integer> hashBaseRound = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseRound.put(baseSolution, 0);
		HashMap<ArrayList<Integer>,Integer> hashBaseSquareToTake = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseSquareToTake.put(baseSolution, baseSolution.get(0));
		HashMap<ArrayList<Integer>,Integer> hashBaseProd = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseProd.put(baseSolution, baseProd);
		HashMap<ArrayList<Integer>,Integer[][]> hashBaseExcessStr = new HashMap<ArrayList<Integer>,Integer[][]>();
		hashBaseExcessStr.put(baseSolution, baseExcessStr);
		
		optMoves.add(hashBaseMove);
		optDirs.add(hashBaseDirection);
		optRounds.add(hashBaseRound);
		optSquareToTake.add(hashBaseSquareToTake);
		optProds.add(hashBaseProd);
		optExcessStr.add(hashBaseExcessStr);
		
		for(int i=1; i<200; i++){
			optMoves.add(new HashMap<ArrayList<Integer>,Integer[][]>());
			optDirs.add(new HashMap<ArrayList<Integer>,Direction[][]>());
			optRounds.add(new HashMap<ArrayList<Integer>,Integer>());
			optSquareToTake.add(new HashMap<ArrayList<Integer>,Integer>());
			optProds.add(new HashMap<ArrayList<Integer>,Integer>());
			optExcessStr.add(new HashMap<ArrayList<Integer>,Integer[][]>());
		}
		//End base case.
		
		//oldMaxRound = maxRound = -1;
		
		//writer.println("Leaving baseCase"); writer.flush();
		
	}
	
	public static void updateNextBaseCase(ArrayList<Integer> nextBaseCase, Integer[][] baseMove, Integer[][] baseExcessStr, Direction[][] baseDirection,int baseProd){//, int desiredRound){
		
		//writer.println("In updateNextBaseCase"); writer.flush();
		
		//Integer[][] baseMove = optMoves.get(nextBaseCase.size()-startingOffset-1).get(nextBaseCase);
		//Integer[][] baseExcessStr = optExcessStr.get(nextBaseCase.size()-startingOffset-1).get(nextBaseCase);
		//Direction[][] baseDirection = optDirs.get(nextBaseCase.size()-startingOffset-1).get(nextBaseCase);
		
		/*writer.println("Next base case:"); writer.flush();
		for(int z=0; z<nextBaseCase.size(); z++) 
			writer.print("(" + nextBaseCase.get(z)%gameMap.width + ", " + nextBaseCase.get(z)/gameMap.width + ")   ");
		
		writer.println(); writer.println();
		
		writer.println("Next Base Move:");writer.flush();
		for(int y=0; y<gameMap.height; y++){
			for(int x=0; x<gameMap.width; x++){
				writer.print(baseMove[x][y]);
				writer.print('\t');
			}
			writer.println();
		}
		
		writer.flush();*/
		
		//int baseProd;
		/*if(optProds.size() < nextBaseCase.size()-startingOffset){
			writer.println("optProds is smaller than optimal solution size."); writer.flush();
			baseProd = 0;
		}
		else if(!optProds.get(nextBaseCase.size()-startingOffset-1).containsKey(nextBaseCase)){
			
			writer.println("optProds hash table doesn't have nextBaseCase."); writer.flush();
			baseProd = 0;
		}
		else{
			baseProd = optProds.get(nextBaseCase.size()-startingOffset-1).get(nextBaseCase);
		}*/
		
		ArrayList<ArrayList<Integer>> baseSolSet = new ArrayList<ArrayList<Integer>>();
		baseSolSet.add(nextBaseCase);
		
		solsByRound = new HashMap<Integer,ArrayList<ArrayList<Integer>> >(); 
		optSolPerRound = new HashMap<Integer,ArrayList<Integer> >();
		optProdPerRound = new HashMap<Integer,Integer>();
		
		for(int i=roundToCompute; i<=endOfGame; i++){
			optProdPerRound.put(i, baseProd);
			optSolPerRound.put(i, nextBaseCase);
			
			if(i==roundToCompute)
				solsByRound.put(i, baseSolSet);
			else
				solsByRound.put(i, new ArrayList<ArrayList<Integer> >());
		}
		
		
		startingOffset = nextBaseCase.size() - 1;
		
		
		HashMap<ArrayList<Integer>,Integer[][]> hashBaseMove = new HashMap<ArrayList<Integer>,Integer[][]>();
		hashBaseMove.put(nextBaseCase, baseMove);
		HashMap<ArrayList<Integer>,Direction[][]> hashBaseDirection = new HashMap<ArrayList<Integer>,Direction[][]>();
		hashBaseDirection.put(nextBaseCase, baseDirection);
		HashMap<ArrayList<Integer>,Integer> hashBaseRound = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseRound.put(nextBaseCase, 0);
		HashMap<ArrayList<Integer>,Integer> hashBaseSquareToTake = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseSquareToTake.put(nextBaseCase, nextBaseCase.get(0));
		HashMap<ArrayList<Integer>,Integer> hashBaseProd = new HashMap<ArrayList<Integer>,Integer>();
		hashBaseProd.put(nextBaseCase, baseProd);
		HashMap<ArrayList<Integer>,Integer[][]> hashBaseExcessStr = new HashMap<ArrayList<Integer>,Integer[][]>();
		hashBaseExcessStr.put(nextBaseCase, baseExcessStr);
		
		
		
		
		optMoves = new ArrayList<HashMap<ArrayList<Integer>,Integer[][]> >();
		optDirs = new ArrayList<HashMap<ArrayList<Integer>,Direction[][]> >();
		optRounds = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
		optSquareToTake = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
		optProds = new ArrayList<HashMap<ArrayList<Integer>,Integer> >();
		optExcessStr = new ArrayList<HashMap<ArrayList<Integer>,Integer[][]> >();
		
		
		optMoves.add(hashBaseMove);
		optDirs.add(hashBaseDirection);
		optRounds.add(hashBaseRound);
		optSquareToTake.add(hashBaseSquareToTake);
		optProds.add(hashBaseProd);
		optExcessStr.add(hashBaseExcessStr);
		
		for(int i=1; i<200; i++){
			optMoves.add(new HashMap<ArrayList<Integer>,Integer[][]>());
			optDirs.add(new HashMap<ArrayList<Integer>,Direction[][]>());
			optRounds.add(new HashMap<ArrayList<Integer>,Integer>());
			optSquareToTake.add(new HashMap<ArrayList<Integer>,Integer>());
			optProds.add(new HashMap<ArrayList<Integer>,Integer>());
			optExcessStr.add(new HashMap<ArrayList<Integer>,Integer[][]>());
		}
		
		
		//maxRound = -1;
		
		//writer.println("Leaving updateNextBaseCase"); writer.flush();
	}//End Next Base Case
	
	
	
	
	
	
	public static ArrayList<ArrayList<Move>> getOptimalMoves(){
		
		ArrayList<ArrayList<Move> > optimalMoves = new ArrayList<ArrayList<Move> >();
		
		if(roundToCompute <= lastRoundComputed+1){
			//writer.println("Game caught up to us."); writer.flush();
			gameCaughtUp = true;
			return new ArrayList<ArrayList<Move>>();
		}
		
		for(int i=lastRoundComputed+1; i<roundToCompute; i++) optimalMoves.add(new ArrayList<Move>());
		
		int bestProd = optProdPerRound.get(roundToCompute-1);
		
		
		
		ArrayList<Integer> optimalSolution = optSolPerRound.get(roundToCompute-1);
		ArrayList<Integer> nextBaseCase = new ArrayList<Integer>(optimalSolution);
		Integer[][] baseMove = new Integer[gameMap.width][gameMap.height];
		Integer[][] baseExcessStr = new Integer[gameMap.width][gameMap.height];
		Direction[][] baseDirection = new Direction[gameMap.width][gameMap.height];
		int baseProd = bestProd;
		
		Integer[][] theMovesForOpt = optMoves.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
		Integer[][] theExcessStr = optExcessStr.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
		Direction[][] theDirs = optDirs.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
		
		for(int y=0; y<gameMap.height; y++){
			for(int x=0; x<gameMap.width; x++){
				baseMove[x][y] = theMovesForOpt[x][y];
				baseExcessStr[x][y] = theExcessStr[x][y];
				baseDirection[x][y] = Direction.STILL;//theDirs[x][y];
			}
		}
		
		/*writer.print("Optimal Solution: ");
		for(int z=0; z<optimalSolution.size(); z++) 
			writer.print("(" + optimalSolution.get(z)%gameMap.width + ", " + optimalSolution.get(z)/gameMap.width + ")   ");
		
		writer.println(); writer.println();
		
		writer.flush();*/
		
		Integer[][] prevMoves = optMoves.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
		
		/*writer.println("Optimal Moves:");writer.flush();
		
		for(int y=0; y<gameMap.height; y++){
			for(int x=0; x<gameMap.width; x++){
				writer.print(prevMoves[x][y] + "\t");
			}
			
			writer.println();
		}
		
		writer.flush();*/
		
		
		
		/*(for(int i=0; i<=maxRound; i++) {
			
			if(optSolPerRound.containsKey(i)){
			
				ArrayList<Integer> theSol = optSolPerRound.get(i);
				int prod = optProds.get(theSol.size()-startingOffset-1).get(theSol);
				//writer.println("Round " + i + ".  Opt: " + prod + ".  Our sol: ");
				
				if(bestProd == prod) break;
			}
//			optimalMoves.add(new ArrayList<Move>());
		}*/
		
		//int maxRound = 0;
		
		for(int i=optimalSolution.size()-startingOffset-1; i>=0; i--){
			
			
			Integer[][] ourMoves = optMoves.get(i).get(optimalSolution);
			Direction[][] ourDirs = optDirs.get(i).get(optimalSolution);
			
			//int optMoveRound = optRounds.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
			int ourProd = optProds.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
			//ArrayList<Integer> theSol = optSolPerRound.get(optRounds.get(optimalSolution.size()-startingOffset-1).get(optimalSolution));
			//int prod = optProds.get(theSol.size()-startingOffset-1).get(theSol);
			//writer.println("Round " + optMoveRound + ".  Opt: " + prod + ".  Our sol: " + ourProd + ". Difference: " + (prod - ourProd));
			
			
			/////
			Integer[][] excess = optExcessStr.get(optimalSolution.size()-startingOffset-1).get(optimalSolution);
			
			int squareTaken = optSquareToTake.get(i).get(optimalSolution);
			
			/*writer.println("Solution we will extend: ");
			for(int z=0; z< optimalSolution.size(); z++)
				writer.print("(" + optimalSolution.get(z)%gameMap.width + ", " + optimalSolution.get(z)/gameMap.width + ")   ");
			
			writer.println();
			
			writer.flush();
			
			
			int roundTaken = ourMoves[squareTaken%gameMap.width][squareTaken/gameMap.width];
			writer.println("Our Str Guess.  Round " + roundTaken);writer.flush();
					
			for(int y=0; y<gameMap.height; y++){
				for(int x=0; x<gameMap.width; x++){
					Site thisSite = gameMap.getSite(new Location(x,y));
					
					if(ourMoves[x][y] == -2){
						writer.print(-1*thisSite.strength);
						writer.print("\t");
					}
					else{
						int strGuess = excess[x][y] + (roundTaken - ourMoves[x][y])*thisSite.production;
						writer.print(strGuess);
						writer.print("\t");
					}
				}
				
				writer.println();
			}
			
			writer.flush();*/
			///////
			
			for(int y=0; y<gameMap.height; y++){
				for(int x=0; x<gameMap.width; x++){
					
					if(ourDirs[x][y] != Direction.STILL){
						
						//if(maxRound < ourMoves[x][y]) maxRound = ourMoves[x][y];
						
						//writer.print("Our Move: " + ourMoves[x][y] + ".  ");writer.flush();
						if(ourMoves[x][y]-(lastRoundComputed+1) >= 0){
							ArrayList<Move> theMoves = optimalMoves.get(ourMoves[x][y]-(lastRoundComputed+1));
							Boolean haveAdded = false;
							for(Move m : theMoves){
								Location l = m.loc;
								
								if(l.x == x && l.y == y){
									//writer.println("Already added direction " + m.dir + " for point (" + x + ", " + y + "), so skipping direction " + ourDirs[x][y]); writer.flush();
									haveAdded=true;
									break;
								}
							}
							
							if(!haveAdded) optimalMoves.get(ourMoves[x][y]-(lastRoundComputed+1)).add(new Move(new Location(x,y),ourDirs[x][y]));
						}
					}
				}
			}
			
			//writer.println("Parsed the crap out of the directions."); writer.flush();
			
			if(i==0){
				
				//writer.println();writer.println();writer.println("Down to i=0.");writer.flush();
				
				//writer.print("Remaining solution: ");
				//for(int z=0; z<optimalSolution.size(); z++) writer.print("(" + optimalSolution.get(z)%gameMap.width + ", " + optimalSolution.get(z)/gameMap.width + ")   ");
				
				//writer.println();
				//writer.flush();
				
				Set<Entry<ArrayList<Integer>, Integer>> solutions = optSquareToTake.get(i).entrySet();
				//writer.println("MUCHO TAQUITO!"); writer.flush();
				Iterator<Entry<ArrayList<Integer>, Integer>> iterator = solutions.iterator();
				
				while(iterator.hasNext()){
					Entry<ArrayList<Integer>, Integer> next = iterator.next();
					ArrayList<Integer> prevSolution = next.getKey();
					
					//writer.print("Solution in HT: ");
					//for(int z=0; z<prevSolution.size(); z++) writer.print("(" + prevSolution.get(z)%gameMap.width + ", " + prevSolution.get(z)/gameMap.width + ")   ");
					
					//writer.println();
					//writer.flush();
				}
				
				//writer.println("No hay mas, imo."); writer.flush();
			}
			
			
			//writer.println("Next square: (" + squareTaken%gameMap.width + ", " + squareTaken/gameMap.width + ")   "); writer.flush();
			int index = optimalSolution.indexOf(squareTaken);			
			optimalSolution.remove(index);
			/*writer.print("Rolled-Back Optimal Solution: ");
			for(int z=0; z<optimalSolution.size(); z++) 
				writer.print("(" + optimalSolution.get(z)%gameMap.width + ", " + optimalSolution.get(z)/gameMap.width + ")   ");
			
			writer.println(); writer.println();
			
			writer.flush();*/
		}
		
		while(optimalMoves.size() > 0 && optimalMoves.get(optimalMoves.size()-1).size() == 0)
			optimalMoves.remove(optimalMoves.size()-1);
		
		
		updateNextBaseCase(nextBaseCase,baseMove,  baseExcessStr, baseDirection, baseProd);
		
		//oldMaxRound = maxRound;
		lastRoundComputed = roundToCompute-1;
		
		return optimalMoves;
    			
	}
	
	
	public static void computeOpt(int myID, int maxtime, int desiredRound){
		
		
		//writer.println("In computeOPT"); writer.flush();
		
		
		//if(true) return new ArrayList<ArrayList<Move> >();
		
		long timestamp = startTime;
		
		Boolean stillHaveTime = true;
		
		
		
		
		while(roundToCompute < endOfGame && stillHaveTime){
			
			//writer.println("Solutions of length " + (i+startingOffset+1) +": " + optMoves.get(i-1).size() + ".  Took " + (System.currentTimeMillis() - timestamp) + " ms.");
			//writer.println("roundToCompute: " + roundToCompute + ".  Timestamp: " + (System.currentTimeMillis() - startTime)); writer.flush();
			timestamp = System.currentTimeMillis();
			
			//Set<Entry<ArrayList<Integer>, Integer[][]>> prevSolutions = optMoves.get(i-1).entrySet();
			//Iterator<Entry<ArrayList<Integer>, Integer[][]>> iterator = prevSolutions.iterator();
			
			/*HashMap<ArrayList<Integer>,Integer[][]> hashNextMove = new HashMap<ArrayList<Integer>,Integer[][]>();
			HashMap<ArrayList<Integer>,Direction[][]> hashNextDirection = new HashMap<ArrayList<Integer>,Direction[][]>();
			HashMap<ArrayList<Integer>,Integer> hashNextProd = new HashMap<ArrayList<Integer>,Integer>();
			HashMap<ArrayList<Integer>,Integer> hashNextRound = new HashMap<ArrayList<Integer>,Integer>();
			HashMap<ArrayList<Integer>,Integer> hashNextSquareToTake = new HashMap<ArrayList<Integer>,Integer>();
			HashMap<ArrayList<Integer>,Integer[][]> hashNextExcessStr = new HashMap<ArrayList<Integer>,Integer[][]>();*/
			
			
			ArrayList<ArrayList<Integer>> thisRound = solsByRound.get(roundToCompute);
			
			Integer[][] newMoves = new Integer[gameMap.width][gameMap.height];
			Direction[][] newDirs = new Direction[gameMap.width][gameMap.height];
			Integer[][] newExcessStr = new Integer[gameMap.width][gameMap.height];
			int[][] distance = new int[gameMap.width][gameMap.height];
			ArrayList<Location> queue = new ArrayList<Location>();
			Boolean[][] haveChecked = new Boolean[gameMap.width][gameMap.height];
			
			//HashSet<ArrayList<Integer>> excessSolutions = new HashSet<ArrayList<Integer>>();
			
			while(thisRound.size() > 0  && stillHaveTime){
				//find adjacent squares and compute optimal time to get this square.
				//Entry<ArrayList<Integer>, Integer[][]> next = iterator.next();
				
				ArrayList<Integer> prevSolution = thisRound.get(0);
				thisRound.remove(0);
				
				int prevProd = optProds.get(prevSolution.size()-startingOffset-1).get(prevSolution);
				
				if(prevProd > optProdPerRound.get(roundToCompute)-10){
					
					Integer[][] prevMoves = optMoves.get(prevSolution.size()-startingOffset-1).get(prevSolution);
					
					
					/*if(roundToCompute<5){
						
						Integer[][] excess = optExcessStr.get(prevSolution.size()-startingOffset-1).get(prevSolution);
						
						writer.println("Solution we will extend: ");
						for(int z=0; z< prevSolution.size(); z++)
							writer.print("(" + prevSolution.get(z)%gameMap.width + ", " + prevSolution.get(z)/gameMap.width + ")   ");
						
						writer.println();
						
						writer.flush();
						
						writer.println("Prev Moves:");writer.flush();
						
						for(int y=0; y<gameMap.height; y++){
							for(int x=0; x<gameMap.width; x++){
								Site thisSite = gameMap.getSite(new Location(x,y));
								
								if(prevMoves[x][y] == -2)
									writer.print(prevMoves[x][y] + ", " + excess[x][y] + ", " + thisSite.strength + "\t");
								else
									writer.print(prevMoves[x][y] + ", " + excess[x][y] + ", " + thisSite.production + "\t");
							}
							
							writer.println();
						}
						
						writer.flush();
					}*/
					
					/*if(i==3){
						writer.println("Solution we will extend: ");
						for(int z=0; z< prevSolution.size(); z++)
							writer.print("(" + prevSolution.get(z)%gameMap.width + ", " + prevSolution.get(z)/gameMap.width + ")   ");
						
						writer.println();
						
						writer.flush();
					}*/
					
					
					
					for(int y=0; y<gameMap.height; y++)
						for(int x=0; x<gameMap.width; x++)
							haveChecked[x][y] = false;
					
					
					
					//Find neighbors of what we own.  If we don't own them, then we consider it as the next square.
					for(int j = 0; j<prevSolution.size() && stillHaveTime; j++){
						
						
						int solnEntry = prevSolution.get(j);
						
						Location currentLoc = new Location(solnEntry%gameMap.width, solnEntry/gameMap.width);
						
						//Check each neighbor of solnEntry and see if it's already in soln.
						for(Direction d : Direction.CARDINALS){
							
							Location neighbor = getNeighborLoc(currentLoc,d);
							
							/*writer.print(neighbor.x);writer.flush();
							
							writer.println("Checking: (" + neighbor.x + ", " + neighbor.y + ").   !haveChecked: " + !haveChecked[neighbor.x][neighbor.y]);// +  " prevMoves: " + prevMoves[neighbor.x][neighbor.y] + ".  !haveChecked: " + !haveChecked[neighbor.x][neighbor.y]);
							writer.flush();*/
							
							//int numAdjacent = 0;
							/*for(Direction nD : Direction.CARDINALS){
								
								Location neighNeighbor = getNeighborLoc(neighbor,nD);
								if(prevMoves[neighNeighbor.x][neighNeighbor.y] != -1) 
									numAdjacent++;
							}*/
							
							if(prevMoves[neighbor.x][neighbor.y] == -2 && !haveChecked[neighbor.x][neighbor.y]){// && numAdjacent < 2){
								//This square isn't in the solution, we haven't considered it yet, and it doesn't complete a square.  Check it's value.
								haveChecked[neighbor.x][neighbor.y] = true;
									
								ArrayList<Integer> newSoln = new ArrayList<Integer>(prevSolution);
								newSoln.add(neighbor.y*gameMap.width + neighbor.x);
								
								/*writer.print("Next Solution: ");
								for(int z=0; z<newSoln.size(); z++) 
									writer.print("(" + newSoln.get(z)%gameMap.width + ", " + newSoln.get(z)/gameMap.width + ")   ");
								
								writer.println(); writer.println();
								
								writer.flush();*/
								
								//Now we want to find the earliest round where we can take neighbor, starting with the same round the previous square was taken.
								//We need to make sure that we don't move a square until after it's last move.
								
								//int prevSquare = optSquareToTake.get(prevSolution.size()-startingOffset-1).get(prevSolution);
								int roundToCheck = roundToCompute;//maxRound + 1 + i;//prevMoves[prevSquare%gameMap.width][prevSquare/gameMap.width];
								
								Integer[][] excessStr = optExcessStr.get(prevSolution.size()-startingOffset-1).get(prevSolution);
								
								Boolean haveSolution = false;
								Site neighborSite = gameMap.getSite(neighbor);
								int strengthNeeded = neighborSite.strength;
								
								//if(roundToCompute<5){
								//	writer.print("Checking (" + neighbor.x + ", " + neighbor.y + ").  strNeeded: " + strengthNeeded + ".  ");
								//}
								
								
								
								while(!haveSolution){
									
									//writer.println("Looking for soln.  Timestamp: " + (System.currentTimeMillis() - startTime)); writer.flush();
									
									//if(roundToCompute<5) writer.print('\n' + "Checking round " + roundToCheck + ".  ");
									int strengthFound = 0;
									
									
									
									//writer.println("Allocated Arrays.  Timestamp: " + (System.currentTimeMillis() - startTime)); writer.flush();
									
									//int newSquareExcessStr = 0;
									
									for(int y=0; y<gameMap.height; y++){
										for(int x=0; x<gameMap.width; x++){
											
											newMoves[x][y] = prevMoves[x][y];
											newDirs[x][y] = Direction.STILL;
											newExcessStr[x][y] = excessStr[x][y];
											distance[x][y] = 0;
										}
									}
									
									//writer.println("Filled in Arrays.  Timestamp: " + (System.currentTimeMillis() - startTime)); writer.flush();
								
								
									//Build BFS Tree
									
									distance[neighbor.x][neighbor.y] = 1;
									newMoves[neighbor.x][neighbor.y] = roundToCheck;//-1;
									
									
									queue.clear();
									queue.add(neighbor);
									
									//writer.println("Queue Created.  Timestamp: " + (System.currentTimeMillis() - startTime)); writer.flush();
									
									Location current;
									
									do{									
										current = queue.get(0);
										//writer.println("BFS: (" + current.x + ", " + current.y + ").  Timestamp: " + (System.currentTimeMillis()-timestamp)); writer.flush();
										
										for(Direction dir : Direction.CARDINALS){
											
											Location currentsNeighbor = getNeighborLoc(current,dir);
								
											if(distance[currentsNeighbor.x][currentsNeighbor.y] == 0 && prevMoves[currentsNeighbor.x][currentsNeighbor.y] > -2 && prevMoves[currentsNeighbor.x][currentsNeighbor.y] + distance[current.x][current.y] <= roundToCheck && distance[current.x][current.y] <= roundToCheck - lastRoundComputed){
												
												distance[currentsNeighbor.x][currentsNeighbor.y] = distance[current.x][current.y] + 1;
												queue.add(currentsNeighbor);
												
												if(strengthFound <= strengthNeeded){
													Site curNeighSite = gameMap.getSite(currentsNeighbor);
													int strAdded = (excessStr[currentsNeighbor.x][currentsNeighbor.y] + (roundToCheck - distance[current.x][current.y] - prevMoves[currentsNeighbor.x][currentsNeighbor.y])*curNeighSite.production);
													//if(roundToCompute<5){
													//	writer.print("(" + currentsNeighbor.x + ", " + currentsNeighbor.y + ") can contribue " + strAdded + ".  ");
													//}
													
													strengthFound += strAdded;//(excessStr[currentsNeighbor.x][currentsNeighbor.y] + (roundToCheck - distance[current.x][current.y] - prevMoves[currentsNeighbor.x][currentsNeighbor.y])*curNeighSite.production);
													newExcessStr[currentsNeighbor.x][currentsNeighbor.y] = 0;
															
													newMoves[currentsNeighbor.x][currentsNeighbor.y] = roundToCheck - distance[current.x][current.y] + 1;
													newDirs[currentsNeighbor.x][currentsNeighbor.y] = oppositeDirection(dir);
												}
											}
											
											if(strengthFound > strengthNeeded){
												
												if(strengthFound > 255)
													strengthFound = 255;
												
												//if(roundToCompute<5) writer.print("Found " + strengthFound + "!  ");
												
												newExcessStr[neighbor.x][neighbor.y] = strengthFound - strengthNeeded;// - neighborSite.production;
												haveSolution = true;
												break;
											}
										}
										
										queue.remove(0);
										
									}while(!queue.isEmpty());
									
									
									if(!haveSolution){
										//if(roundToCompute<5) writer.print("Only found " + strengthFound + ".  ");
										roundToCheck++;
									}
									else{
										//We have found our solution.  If it is the best for this subset, then add it to the hash table.
										
										//Check to see if this subset has been computed before.  Only keep the best one.
										//If this one is the best for the subset and the last square was found in the same round as next-to-last, then we need to add to a set to be searched again.
										
										int z=newSoln.size()-2;
										while(z >= 0 && newSoln.get(z) > newSoln.get(z+1)){
											int tmp = newSoln.get(z+1);
											newSoln.set(z+1, newSoln.get(z));
											newSoln.set(z, tmp);
											
											z--;
										}
										
										if(!optRounds.get(newSoln.size()-startingOffset-1).containsKey(newSoln) || optRounds.get(newSoln.size()-startingOffset-1).get(newSoln) > roundToCheck){
											
											Integer[][] newMoves2 = new Integer[gameMap.width][gameMap.height];
											Direction[][] newDirs2 = new Direction[gameMap.width][gameMap.height];
											Integer[][] newExcessStr2 = new Integer[gameMap.width][gameMap.height];
											
	
											for(int y=0; y<gameMap.height; y++){
												for(int x=0; x<gameMap.width; x++){
													
													newMoves2[x][y] = newMoves[x][y];
													newDirs2[x][y] = newDirs[x][y];
													newExcessStr2[x][y] = newExcessStr[x][y];
												}
											}
											
											if(optRounds.get(newSoln.size()-startingOffset-1).containsKey(newSoln))// && solsByRound.get(hashNextRound.get(newSoln)).contains(newSoln))
													solsByRound.get(optRounds.get(newSoln.size()-startingOffset-1).get(newSoln)).remove(newSoln);
											
											//if(roundToCompute<5){ writer.println("roundToCheck for (" + neighbor.x + ", " + neighbor.y + "): " + roundToCheck); writer.flush();}
											
											solsByRound.get(roundToCheck).add(newSoln);
											
											/*if(roundToCheck == i){
												thisRound.add(newSoln);
											}*/
										
											optMoves.get(newSoln.size()-startingOffset-1).put(newSoln, newMoves2);
											optDirs.get(newSoln.size()-startingOffset-1).put(newSoln, newDirs2);
											
											int newProd = prevProd + neighborSite.production;
											optProds.get(newSoln.size()-startingOffset-1).put(newSoln, newProd);
											
											optRounds.get(newSoln.size()-startingOffset-1).put(newSoln, roundToCheck);
											optSquareToTake.get(newSoln.size()-startingOffset-1).put(newSoln, neighbor.y*gameMap.width + neighbor.x);
											
											optExcessStr.get(newSoln.size()-startingOffset-1).put(newSoln, newExcessStr2);
											
											int optFor = roundToCheck;
											
											while(optFor <= desiredRound && optProdPerRound.get(optFor) <= newProd){
												optProdPerRound.put(optFor, newProd);
												optSolPerRound.put(optFor, newSoln);
												optFor++;
											}
											
											
											
											
											/*writer.println("Next Move: ");
											for(int y=0; y<gameMap.height; y++){			
												for(int x=0; x<gameMap.width; x++){
													
													writer.print(newMoves[x][y]);
													writer.print('\t');
												}
												
												writer.println();
											}
											
											writer.println(); writer.println();
											
											writer.flush();*/
										}
									}
									
									
								
								}
								
								
								
									
								
								
								
							}
							
						}
						
						
						
					}//End of finding all extensions of a previous solution.
				
				}
				
				if(System.currentTimeMillis() - startTime > maxtime*1000 - 200){
					stillHaveTime = false;
					//maxRound = maxRound + i-1;
					//writer.println("Out of time.  Returning."); writer.flush();
					return;
				}
				
				/*if(!iterator.hasNext() && excessSolutions.size() > 0){
				
					writer.println("Have excess!  Going back to look again."); writer.flush();
					
					iterator = excessSolutions.iterator();
					
					writer.print("Iterator Before: " + iterator.hasNext()); writer.flush();
				
					excessSolutions = new HashSet<ArrayList<Integer>>();
					

					writer.println("  Iterator After: " + iterator.hasNext()); writer.flush();
				}*/
				
				
			}//Finished extending i-1 solns to i solutions.
			
			/*optMoves.add(hashNextMove);
			optDirs.add(hashNextDirection);
			optProds.add(hashNextProd);
			optRounds.add(hashNextRound);
			optSquareToTake.add(hashNextSquareToTake);
			optExcessStr.add(hashNextExcessStr);*/
			
			//maxRound = i;
			
			roundToCompute++;
			
		}
		
		
		
		
		//Now we have computed all of the optimal solutions.  We now are ready to construct our set of Moves.
		
		//while(!optProdPerRound.containsKey(desiredRound)) desiredRound--;
		
		
    }//End computeOPT

    
	public static Location getNeighborLoc(Location loc, Direction d){
		
		
		//writer.print("getNeighborLoc"); writer.flush();
		int neighborX = loc.x, neighborY = loc.y;
		
		if(d == Direction.EAST){
			neighborX++;
			
			if(neighborX >= gameMap.width) neighborX = 0;
		}
		else if(d == Direction.WEST){
			neighborX--;
			
			if(neighborX < 0) neighborX = gameMap.width-1;
		}
		else if(d == Direction.NORTH){
			neighborY--;
			
			if(neighborY<0) neighborY = gameMap.height-1;
		}
		else{
			neighborY++;
			
			if(neighborY>=gameMap.height) neighborY = 0;
		}
		
		 //writer.print("  Returning: (" + neighborX + ", " + neighborY + ").  "); writer.flush();
		return new Location(neighborX,neighborY);
		
	}
	
	
	public static void copyMap(Integer[][] map1, Integer[][] map2){
		
		for(int y=0; y<gameMap.height; y++){
			for(int x=0; x<gameMap.width; x++){
				
				map2[x][y] = map1[x][y];
			}
		}
	}
	
	public static Direction oppositeDirection(Direction d){
		
		if(d == Direction.NORTH) return Direction.SOUTH;
		
		if(d == Direction.SOUTH) return Direction.NORTH;
		
		if(d==Direction.EAST) return Direction.WEST;
		
		return Direction.EAST;
	}
	
	
	public static ArrayList<Move> killMode(int myID, int[][] distToBorder, int[][] strengthNeeded, int round){
		ArrayList<Move> moves = new ArrayList<Move>();
		
		findBorder(myID, distToBorder, strengthNeeded);
		shortestPaths(myID, distToBorder, strengthNeeded);
		
		for(int y=0; y<gameMap.height; y++){
			for(int x=0; x<gameMap.width; x++){
				
				if(distToBorder[x][y] != -1){
					
					int xMult = 1;
					
					Location wNeighbor = getNeighborLoc(new Location(x,y),Direction.WEST);
					Location eNeighbor = getNeighborLoc(new Location(x,y),Direction.EAST);
					
					if(distToBorder[wNeighbor.x][wNeighbor.y] < distToBorder[eNeighbor.x][eNeighbor.y])
						xMult = -1;
					
					int yMult = 1;
					
					Location nNeighbor = getNeighborLoc(new Location(x,y),Direction.WEST);
					Location sNeighbor = getNeighborLoc(new Location(x,y),Direction.EAST);
					
					if(distToBorder[nNeighbor.x][nNeighbor.y] < distToBorder[sNeighbor.x][sNeighbor.y])
						yMult = -1;
					
					
					
					if(round%4 == (xMult*x+yMult*y)%4){
						
						for(Direction d : Direction.CARDINALS){
							
							Location neighbor = getNeighborLoc(new Location(x,y),d);
							if(distToBorder[neighbor.x][neighbor.y] < distToBorder[x][y]){
								moves.add(new Move(new Location(x,y),d));
								break;
							}
						}
						
					}
					
				}
				
			}
		}
		
		
		return moves;
	}
	
	
	public static ArrayList<Move> greedy(int myID, int[][] distToBorder, int[][] strengthNeeded){
    	ArrayList<Move> moves = new ArrayList<Move>();
    	
    	//int[][] oldDists = Arrays.CopdistToBorder    	
    	moves.addAll(findBorder(myID, distToBorder, strengthNeeded));
    	moves.addAll(shortestPaths(myID, distToBorder, strengthNeeded));
    	
    	
    	
    	return moves;
    }
    
    public static  ArrayList<Move> findBorder(int myID, int[][] distToBorder, int[][] strengthNeeded){
    	ArrayList<Move> moves = new ArrayList<Move>();
    	
    	for(int y = 0; y < gameMap.height; y++) {
            for(int x = 0; x < gameMap.width; x++) {
            	
                Site site = gameMap.getSite(new Location(x, y));
                
                if(site.owner == myID) {
                	
                	Boolean onBorder = false;
                	int minStr = 9999;
                	
                	Direction ourDir = Direction.STILL;
                	for(Direction d : Direction.CARDINALS){
                		
                		Site neighbor = gameMap.getSite(new Location(x,y), d);
                		
                		if(neighbor.owner != myID){
                			
                			onBorder = true;
                			if(neighbor.strength < minStr){
                				minStr = neighbor.strength;
                				ourDir = d;
                			}
                				
                		}
                		               			                		
                	}
                	
                	if(onBorder){
            			distToBorder[x][y] = 1;
            			strengthNeeded[x][y] = minStr - site.strength;
            			if(strengthNeeded[x][y] <= 0){
            				moves.add(new Move(new Location(x,y),ourDir));
            				
            				int neighborX = x, neighborY = y;
            				
            				if(ourDir == Direction.EAST){
            					neighborX++;
            					
            					if(neighborX >= gameMap.width) neighborX = 0;
            				}
            				else if(ourDir == Direction.WEST){
            					neighborX--;
            					
            					if(neighborX < 0) neighborX = gameMap.width-1;
            				}
            				else if(ourDir == Direction.NORTH){
            					neighborY--;
            					
            					if(neighborY<0) neighborY = gameMap.height-1;
            				}
            				else{
            					neighborY++;
            					
            					if(neighborY>=gameMap.height) neighborY = 0;
            				}
            				
            				
            				int minNeighborStr = 250;
            				for(Direction d : Direction.CARDINALS){
            					
            					Site neighborsNeighbor = gameMap.getSite(new Location(neighborX,neighborY), d);
            					
            					if(neighborsNeighbor.owner != myID && neighborsNeighbor.strength < minNeighborStr)
            						minNeighborStr = neighborsNeighbor.strength;
            				}
            				
            				
            				strengthNeeded[x][y] = minNeighborStr;
            				
            			}
            			else
            				moves.add(new Move(new Location(x,y),Direction.STILL));
            		}
            		else{
            			distToBorder[x][y] = 9999;
                    	strengthNeeded[x][y] = 99999;
            		}
                }
                else{
                	distToBorder[x][y] = -1;
                	strengthNeeded[x][y] = -1;
                }
            }
        }
    	
    	return moves;
    	
    }
    
    
    public static ArrayList<Move> shortestPaths(int myID, int[][] distToBorder, int[][] strengthNeeded){
    	ArrayList<Move> moves = new ArrayList<Move>();
    	
    	Boolean is9999 = true;
    	int currentDist = 2;
    	int MaxDist = 1;
    	
    	while(is9999){
    		
    		is9999 = false;
    		
    		for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                	
                	if(distToBorder[x][y] == 9999){
                		
                		MaxDist = currentDist;
                		
                		int xToCheck = x+1;
                		if(xToCheck >= gameMap.width) xToCheck = 0;
                		
                		Direction firstDir = Direction.STILL;
                		Direction secondDir = Direction.STILL;
                		
                		if(distToBorder[xToCheck][y] == currentDist-1){
                			distToBorder[x][y] = currentDist;
                			
                			Location loc = new Location(x,y);
                			Site site = gameMap.getSite(loc);
                			Site neighbor = gameMap.getSite(loc,Direction.EAST);
                			
                			if(strengthNeeded[xToCheck][y] - neighbor.production > 0){
                				if(strengthNeeded[xToCheck][y] - neighbor.production  < strengthNeeded[x][y]){
                					strengthNeeded[x][y] = strengthNeeded[xToCheck][y] - neighbor.production;
                					
                					if(site.strength > strengthNeeded[xToCheck][y] - neighbor.production)
                						firstDir = Direction.EAST;
                						//moves.add(new Move(loc,Direction.EAST));
                				}
                			}
                			else{
                				secondDir = Direction.EAST;
                			}
                		}
                		
                		
                		
                		xToCheck = x-1;
                		if(xToCheck < 0) xToCheck = gameMap.width-1;
                			
                		if(distToBorder[xToCheck][y] == currentDist-1){
                			distToBorder[x][y] = currentDist;
                			
                			Location loc = new Location(x,y);
                			Site site = gameMap.getSite(loc);
                			Site neighbor = gameMap.getSite(loc,Direction.WEST);
                			
                			if(strengthNeeded[xToCheck][y] - neighbor.production > 0){
                				if(strengthNeeded[xToCheck][y] - neighbor.production  < strengthNeeded[x][y])
                					strengthNeeded[x][y] = strengthNeeded[xToCheck][y] - neighbor.production;
                				
                				if(site.strength > strengthNeeded[xToCheck][y] - neighbor.production)
                					firstDir = Direction.WEST;
        						//moves.add(new Move(loc,Direction.WEST));
                			}
                			else{
                				secondDir = Direction.WEST;
                			}
                		}
                		
                			
                		int yToCheck = y+1;
                		if(yToCheck >= gameMap.height) yToCheck = 0;
                		
                		if(distToBorder[x][yToCheck] == currentDist-1){
                			distToBorder[x][y] = currentDist;
                			
                			Location loc = new Location(x,y);
                			Site site = gameMap.getSite(loc);
                			Site neighbor = gameMap.getSite(loc,Direction.SOUTH);
                			
                			if(strengthNeeded[x][yToCheck] - neighbor.production > 0){
                				if(strengthNeeded[x][yToCheck] - neighbor.production  < strengthNeeded[x][y])
                					strengthNeeded[x][y] = strengthNeeded[x][yToCheck] - neighbor.production;
                				
                				if(site.strength > strengthNeeded[x][yToCheck] - neighbor.production)
                					firstDir = Direction.SOUTH;
        						//moves.add(new Move(loc,Direction.SOUTH));
                			}
                			else{
                				secondDir = Direction.SOUTH;
                			}
                		}
                		
                		
                		yToCheck = y-1;
                		if(yToCheck < 0) yToCheck = gameMap.height -1;
                			
                		if(distToBorder[x][yToCheck] == currentDist-1){
                			distToBorder[x][y] = currentDist;
                			
                			Location loc = new Location(x,y);
                			Site site = gameMap.getSite(loc);
                			Site neighbor = gameMap.getSite(loc,Direction.NORTH);
                			
                			if(strengthNeeded[x][yToCheck] - neighbor.production > 0){
                				if(strengthNeeded[x][yToCheck] - neighbor.production  < strengthNeeded[x][y])
                					strengthNeeded[x][y] = strengthNeeded[x][yToCheck] - neighbor.production;
                				
                				if(site.strength > strengthNeeded[x][yToCheck] - neighbor.production)
                					firstDir = Direction.NORTH;
        						//moves.add(new Move(loc,Direction.NORTH));
                			}
                			else{
                				secondDir = Direction.NORTH;
                			}
                		}
                			
                		
                		
                		
                		if(distToBorder[x][y] == 9999) 
                			is9999=true;
                		else if(distToBorder[x][y] > 5 && gameMap.getSite(new Location(x,y)).strength < 100)
                			moves.add(new Move(new Location(x,y),Direction.STILL));
                		else if(firstDir != Direction.STILL)
                			moves.add(new Move(new Location(x,y),firstDir));
                		else if(secondDir != Direction.STILL){
                			moves.add(new Move(new Location(x,y),secondDir));
                			strengthNeeded[x][y] = -1;
                		}
                	}
                	
                }
                
             }
    		
    		currentDist++;
    	}
    	
    	return moves;
    }

    
    
    
    
}
