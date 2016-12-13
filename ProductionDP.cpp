#include <stdlib.h>
#include <time.h>
#include <cstdlib>
#include <ctime>
#include <time.h>
#include <set>
#include <tr1/unordered_map>
#include <vector>
#include <queue> 
#include <fstream>
#include <chrono>
#include <boost/functional/hash.hpp>

#include "hlt.hpp"
#include "networking.hpp"

using namespace std;
using namespace hlt;
using namespace tr1;
using ms = chrono::milliseconds;
using get_time = chrono::steady_clock;

template <typename Container>
struct solutionHash{
  
  size_t operator()(Container const& c) const{
    return boost::hash_range(c.begin(),c.end());
  }
};


GameMap gameMap;
unsigned char myID;
ofstream writer;
int mapWidth;
int mapHeight;
vector<unordered_map<vector<int>,vector<int>, solutionHash<vector<int> > > > optMoves;
vector<unordered_map<vector<int>,vector<unsigned char>, solutionHash<vector<int> > > > optDirs;
vector<unordered_map<vector<int>,int, solutionHash<vector<int> > > > optRounds;
vector<unordered_map<vector<int>,int, solutionHash<vector<int> > > > optSquareToTake;
vector<unordered_map<vector<int>,int, solutionHash<vector<int> > > > optProds;
vector<unordered_map<vector<int>,vector<int>, solutionHash<vector<int> > > > optExcessStr;

unordered_map<int, vector<vector<int> > > solsByRound;
unordered_map<int, int> optProdPerRound;
unordered_map<int, vector<int> > optSolPerRound;

int startingOffset;
get_time::time_point startTime;
int roundToCompute;
int endOfGame;
int lastRoundComputed;
bool gameCaughtUp;
int radius;

void baseCase();
void updateNextBaseCase(vector<int> nextBaseCase, vector<int> baseMove, vector<int> baseExcessStr, vector<unsigned char> baseDirection, int baseProd);
vector<set<Move> > getOptimalMoves();
void computeOpt(int maxtime);
Location getNeighborLoc(Location loc, unsigned char d);
unsigned char oppositeDirection(unsigned char dir);

int main() {
    srand(time(NULL));

    cout.sync_with_stdio(0);
    writer.open("HaliteLog.txt");
    
    getInit(myID, gameMap);
    
    startTime = get_time::now();
    endOfGame = 10*sqrt(gameMap.width*gameMap.height);
    gameCaughtUp = false;
    
    baseCase();
    computeOpt(14);
    vector<set<Move> > optimalMoves;// = getOptimalMoves();
    int solIndex = 0;
    
    sendInit("GibsonUTSA");

    //set<Move> moves;
    while(true) {
        //moves.clear();

        getFrame(gameMap);
	startTime = get_time::now();
	
	if(gameCaughtUp){
	  
	  set<Move> moves;
	  sendFrame(moves);
	}
	else{
	  
	  if(solIndex >= optimalMoves.size()){
	   
	    optimalMoves = getOptimalMoves();
	    solIndex = 0;
	  }
	  
	  computeOpt(1);
	  
	  if(solIndex >= optimalMoves.size()){
	    
	    set<Move> moves;
	    sendFrame(moves);
	  }
	  else{
	    
	    sendFrame(optimalMoves[solIndex]);
	    solIndex++;	    
	    
	  }
	  
	  
	}

    }

    return 0;
}




void baseCase(){
  
 roundToCompute = 0;
 lastRoundComputed = -1;
 radius = 1;
 
 vector<int> baseSolution;
 vector<int> baseMove;
 vector<int> baseExcessStr;
 vector<unsigned char> baseDirection;
 int baseProd = 0;
 
 for(unsigned short y=0; y<gameMap.height; y++){
   for(unsigned short x=0; x<gameMap.width; x++){
     
     baseDirection.push_back(STILL);
     
     if(gameMap.getSite({ x, y }).owner == myID) {
        
       baseSolution.push_back(y*gameMap.width + x);
       baseMove.push_back(-1);
       baseProd += gameMap.getSite({ x, y }).production;
       baseExcessStr.push_back(gameMap.getSite({ x, y }).strength);
     }
     else{
      
       baseMove.push_back(-2);
       baseExcessStr.push_back(0);
     }
     
   }
 }
 
 
 vector<vector<int> > baseSolSet;
 baseSolSet.push_back(baseSolution);
  
 
 for(int i=0; i<endOfGame; i++){
   
  optProdPerRound[i] = baseProd;
  optSolPerRound[i] = baseSolution;
  
  if(i==0)
    solsByRound[i] = baseSolSet;
  else{
   
    vector<vector <int> > newList;
    solsByRound[i] = newList;
  }
 }
  
  startingOffset = baseSolution.size() - 1;
  
  unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseMove;
  hashBaseMove[baseSolution] = baseMove;
  unordered_map<vector<int>, vector<unsigned char>, solutionHash<vector<int> > > hashBaseDirection;
  hashBaseDirection[baseSolution] = baseDirection;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseRound;
  hashBaseRound[baseSolution] = 0;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseSquareToTake;
  hashBaseSquareToTake[baseSolution] = 0;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseProd;
  hashBaseProd[baseSolution] = baseProd;
  unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseExcessStr;
  hashBaseExcessStr[baseSolution] = baseExcessStr;
  
  optMoves.push_back(hashBaseMove);
  optDirs.push_back(hashBaseDirection);
  optRounds.push_back(hashBaseRound);
  optSquareToTake.push_back(hashBaseSquareToTake);
  optProds.push_back(hashBaseProd);
  optExcessStr.push_back(hashBaseExcessStr);
  
  for(int i=1; i<200; i++){
    
    unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseMove2;
    unordered_map<vector<int>, vector<unsigned char>, solutionHash<vector<int> > > hashBaseDirection2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseRound2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseSquareToTake2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseProd2;
    unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseExcessStr2;
    
    optMoves.push_back(hashBaseMove2);
    optDirs.push_back(hashBaseDirection2);
    optRounds.push_back(hashBaseRound2);
    optSquareToTake.push_back(hashBaseSquareToTake2);
    optProds.push_back(hashBaseProd2);
    optExcessStr.push_back(hashBaseExcessStr2);
    
  }
  
}


void updateNextBaseCase(vector<int> nextBaseCase, vector<int> baseMove, vector<int> baseExcessStr, vector<unsigned char> baseDirection, int baseProd){
  
  //writer << "In nextBaseCase" << endl;
  
  optMoves.clear();
  optDirs.clear();
  optRounds.clear();
  optSquareToTake.clear();
  optProds.clear();
  optExcessStr.clear();
  
  solsByRound.clear();
  optSolPerRound.clear();
  optProdPerRound.clear();
  
  vector<vector<int> > baseSolSet;
  baseSolSet.push_back(nextBaseCase);
  
  for(int i=roundToCompute; i<=endOfGame; i++){
   optProdPerRound[i] = baseProd;
   optSolPerRound[i] = nextBaseCase;
   
   if(i==roundToCompute)
     solsByRound[i] = baseSolSet;
   else{
    
     vector<vector<int> > theSols;
     solsByRound[i] = theSols;
   }
    
  }
  
  startingOffset = nextBaseCase.size()-1;
  
  unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseMove;
  hashBaseMove[nextBaseCase] = baseMove;
  unordered_map<vector<int>, vector<unsigned char>, solutionHash<vector<int> > > hashBaseDirection;
  hashBaseDirection[nextBaseCase] = baseDirection;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseRound;
  hashBaseRound[nextBaseCase] = 0;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseSquareToTake;
  hashBaseSquareToTake[nextBaseCase] = 0;
  unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseProd;
  hashBaseProd[nextBaseCase] = baseProd;
  unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseExcessStr;
  hashBaseExcessStr[nextBaseCase] = baseExcessStr;
  
  optMoves.push_back(hashBaseMove);
  optDirs.push_back(hashBaseDirection);
  optRounds.push_back(hashBaseRound);
  optSquareToTake.push_back(hashBaseSquareToTake);
  optProds.push_back(hashBaseProd);
  optExcessStr.push_back(hashBaseExcessStr);
  
  for(int i=1; i<200; i++){
    
    unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseMove2;
    unordered_map<vector<int>, vector<unsigned char>, solutionHash<vector<int> > > hashBaseDirection2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseRound2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseSquareToTake2;
    unordered_map<vector<int>, int, solutionHash<vector<int> > > hashBaseProd2;
    unordered_map<vector<int>, vector<int>, solutionHash<vector<int> > > hashBaseExcessStr2;
    
    optMoves.push_back(hashBaseMove2);
    optDirs.push_back(hashBaseDirection2);
    optRounds.push_back(hashBaseRound2);
    optSquareToTake.push_back(hashBaseSquareToTake2);
    optProds.push_back(hashBaseProd2);
    optExcessStr.push_back(hashBaseExcessStr2);
    
  }
  
  
  //compute radius of our region.
  vector<int> ourDistance, oursNotFound;
  
  //First find boundary.
  for(unsigned short y=0; y<gameMap.height; y++){
    for(unsigned short x=0; x<gameMap.width; x++){
      
	int index = x + y*gameMap.width;
	if(baseMove[index] == -2){
	  //(x,y) isn't ours
	  ourDistance.push_back(-2); 
	}
	else{
	  //(x,y) is ours.  Is it on boundary?
	  
	  bool onBoundary = false;
	  
	  for(unsigned char d=1; d<=4; d++){
	    
	    Location neighbor = getNeighborLoc({x,y},d);
	    int neighborIndex = neighbor.x + neighbor.y*gameMap.width;
	    
	    if(baseMove[neighborIndex] == -2) 
	      onBoundary = true;
	    
	  }
	  
	  if(onBoundary){
	    //writer << "(" << index%gameMap.width << ", " << index/gameMap.width << ") is on the boundary." << endl;
	    ourDistance.push_back(1);
	  }
	  else{
	    //writer << "(" << index%gameMap.width << ", " << index/gameMap.width << ") is not on the boundary." << endl;
	    ourDistance.push_back(0);
	    oursNotFound.push_back(index);
	  }
	}
	
    }
  }
  
  //Currently: Not ours = -2.  Boundary = 1.  Ours but not boundary = 0.
  radius = 1;
  if(!oursNotFound.empty()) radius++;
  vector<int> excess;
  
  while(!oursNotFound.empty()){
   
    int square = oursNotFound[oursNotFound.size()-1];
    oursNotFound.pop_back();
    Location sLoc = { (unsigned short)(square%gameMap.width), (unsigned short)(square/gameMap.width) };
    
    //writer << "Checking: (" << square%gameMap.width << ", " << square/gameMap.width << ")." << endl;
    
    bool extended = false;
    
    for(unsigned char d=1; d<=4; d++){
      
      Location neighbor = getNeighborLoc(sLoc,d);
      int neighborIndex = neighbor.x + neighbor.y*gameMap.width;
	    
      if(ourDistance[neighborIndex] == radius-1){
	ourDistance[square] = radius;
	extended = true;
      }
      
    }
    
    if(!extended){
     excess.push_back(square); 
     //writer << "(" << square%gameMap.width << ", " << square/gameMap.width << ") is farther than distance " << radius << "." << endl;
    }
    else{
      //writer << "(" << square%gameMap.width << ", " << square/gameMap.width << ") is distance " << radius << "." << endl;
    }
    
    
    if(oursNotFound.empty() && !excess.empty()){
     
      //writer << "Radius is larger than " << radius << ".  Going back." << endl;
      radius++;
      oursNotFound = excess;
      excess.clear();
    }
    
    
  }
  
  
  //writer << "New radius: " << radius << endl;
  
  
  
  //writer << "Leaving nextBaseCase" << endl;
}



vector<set<Move> > getOptimalMoves(){
  
  //writer << "In getOptimalMoves." << endl;
  
  vector<set<Move> > optimalMoves;
  
  if(roundToCompute - (lastRoundComputed+1) < radius){
   
    gameCaughtUp = true;
    return optimalMoves;
  }
  
  for(int i=lastRoundComputed+1; i<roundToCompute; i++){
   
    set<Move> theMoves;
    optimalMoves.push_back(theMoves);
  }
  
  //writer << "Added empty sets. " << endl;
  
  vector<int> optimalSolution = optSolPerRound[roundToCompute-1];
  vector<int> nextBaseCase;
  for(int i=0; i<optimalSolution.size(); i++) nextBaseCase.push_back(optimalSolution[i]);
  
  
  vector<int> theMovesForOpt = optMoves[optimalSolution.size()-startingOffset-1][optimalSolution];
  vector<int> theExcessStr = optExcessStr[optimalSolution.size()-startingOffset-1][optimalSolution];
  int baseProd = optProdPerRound[roundToCompute-1];
  
  
  //writer << "Optimal Solution:" << endl;
  //for(int z = 0; z<optimalSolution.size(); z++) writer << "(" << optimalSolution[z]%gameMap.width << ", " << optimalSolution[z]/gameMap.width << "), ";
  //writer << endl;
  
  
  vector<int> baseMove;
  vector<int> baseExcessStr;
  vector<unsigned char> baseDirection;
  
  for(int y=0; y<gameMap.height; y++){
    for(int x=0; x<gameMap.width; x++){
      
      baseMove.push_back(theMovesForOpt[y*gameMap.width + x]);
      baseExcessStr.push_back(theExcessStr[y*gameMap.width + x]);
      baseDirection.push_back(STILL);
    }
  }
  
  
  for(int i=optimalSolution.size()-startingOffset-1; i>=0; i--){
    
    //writer << "Opt sol square " << i << endl;
    
    vector<int> ourMoves = optMoves[i][optimalSolution];
    vector<unsigned char> ourDirs = optDirs[i][optimalSolution];
    
    int squareTaken = optSquareToTake[i][optimalSolution];
    
    for(unsigned short y=0; y<gameMap.height; y++){
      for(unsigned short x=0; x<gameMap.width; x++){
      
	if(ourDirs[y*gameMap.width + x] != STILL){
	  
	 optimalMoves[ourMoves[y*gameMap.width + x] - (lastRoundComputed+1)].insert({ { x, y }, ourDirs[y*gameMap.width + x] });
	  
	  
	  
	  
	  
	}
	
	
      }
    
    }
    
    
    auto it = find(optimalSolution.begin(), optimalSolution.end(), squareTaken);
    if(it != optimalSolution.end())
      optimalSolution.erase(it);
    
    
  }
  
  
  updateNextBaseCase(nextBaseCase,baseMove,  baseExcessStr, baseDirection, baseProd);
		

  lastRoundComputed = roundToCompute-1;
  
  //writer << "Leaving getOptimalMoves." << endl;
		
  return optimalMoves;
  
  
}



void computeOpt(int maxtime){
  
  //writer << "In comptueOpt()"  << endl;
  
  bool stillHaveTime = true;
 
		
  while(roundToCompute < endOfGame && stillHaveTime){
    
    get_time::time_point time = get_time::now();
    auto duration = time - startTime;
      
    
    
    vector<vector<int> > thisRound = solsByRound[roundToCompute];
    //writer << "roundToCompute: " << roundToCompute << ".  Time: " << chrono::duration_cast<ms>(duration).count() << ".  Solutions: " << solsByRound[roundToCompute].size()<< endl;
    
    while(solsByRound[roundToCompute].size()>0 && stillHaveTime){
      
      vector<int> prevSolution = solsByRound[roundToCompute][0];
      
      //writer << "Solution extending:" << endl;
      //for(int z = 0; z<prevSolution.size(); z++) writer << "(" << prevSolution[z]%gameMap.width << ", " << prevSolution[z]/gameMap.width << "), ";
      //writer << endl;
      
      swap(solsByRound[roundToCompute][0],solsByRound[roundToCompute].back());
      solsByRound[roundToCompute].pop_back();
      
      int prevProd = optProds[prevSolution.size()-startingOffset-1][prevSolution];
      
      //writer << "PrevProd: " << prevProd << ".  Opt: " << optProdPerRound[roundToCompute] << endl;
      
      if(prevProd > optProdPerRound[roundToCompute]-10){
	
	
	
	
	vector<int> prevMoves = optMoves[prevSolution.size()-startingOffset-1][prevSolution];
	
	vector<bool> haveChecked;
	for(unsigned short y=0; y<gameMap.height; y++)
	  for(unsigned short x=0; x<gameMap.width; x++)
	    haveChecked.push_back(false);
	  
	  
	for(int j=0; j<prevSolution.size() && stillHaveTime; j++){
	  
	  
	  int solnEntry = prevSolution[j];
	  
	  Location currentLoc = { (unsigned short)(solnEntry%gameMap.width), (unsigned short)(solnEntry/gameMap.width) };
	  
	  for(unsigned char d=1; d<=4; d++){
	    
	    Location neighbor = getNeighborLoc(currentLoc,d);
	    int neighIndex = neighbor.x + gameMap.width*neighbor.y;
	    
	    if(prevMoves[neighIndex] == -2 && !haveChecked[neighIndex]){
	      
	      
	      
	      haveChecked[neighIndex] = true;
	      
	      vector<int> newSoln;
	      for(int z=0; z<prevSolution.size(); z++) newSoln.push_back(prevSolution[z]);
	      
	      newSoln.push_back(neighIndex);
	      
	      int roundToCheck = roundToCompute;
	      
	      vector<int> excessStr = optExcessStr[prevSolution.size()-startingOffset-1][prevSolution];
	      
	      bool haveSolution = false;
	      Site neighborSite = gameMap.getSite(neighbor);
	      int strengthNeeded = neighborSite.strength;
	      
	      //writer << "Checking (" << neighbor.x << ", " << neighbor.y << ").  Str Needed: "<< strengthNeeded << endl;
	      
	      while(!haveSolution){
		
		//writer << "roundToCheck: " << roundToCheck << endl;
		
		int strengthFound = 0;
		
		
		
		vector<int> newMoves;
		vector<unsigned char> newDirs;
		vector<int> newExcessStr;
		vector<int> distance;
		
		for(int y=0; y<gameMap.height; y++){
		  for(int x=0; x<gameMap.width; x++){
		   
		    newMoves.push_back(prevMoves[y*gameMap.width + x]);
		    newDirs.push_back(STILL);
		    newExcessStr.push_back(excessStr[y*gameMap.width + x]);
		    distance.push_back(0);
		    
		  } 
		}
		
		
		distance[neighIndex] = 1;
		newMoves[neighIndex] = roundToCheck;
		
		
		queue<Location> theQueue;
		theQueue.push(neighbor);
		
		//writer << "theQueue size: " << theQueue.size() << endl;
		
		Location current;
		
		while(!theQueue.empty()){
		  
		  //writer << "In BFS Loop. " << endl;
		  
		  current = theQueue.front();
		  theQueue.pop();
		  int curIndex = current.x + gameMap.width*current.y;
		  
		  //writer << "Current: (" << current.x << ", " << current.y << ")." << endl;
		  
		  for(unsigned char dir = 1; dir <= 4 && strengthNeeded >= strengthFound; dir++){
		    
		    //writer << "dir: " << dir << "  " << flush;
		    
		    Location currentsNeighbor = getNeighborLoc(current,dir);
		    int curNeighIndex = currentsNeighbor.x + gameMap.width*currentsNeighbor.y;
		    
		    if(distance[curNeighIndex] == 0 && prevMoves[curNeighIndex] > -2 && prevMoves[curNeighIndex] + distance[curIndex] <= roundToCheck && distance[curIndex] <= roundToCheck - lastRoundComputed){
		      
		      //writer << "Enqueing (" << currentsNeighbor.x << ", " << currentsNeighbor.y << ").  " << flush;
		      distance[curNeighIndex] = distance[curIndex] + 1;
		      theQueue.push(currentsNeighbor);
		      
		      if(strengthFound <= strengthNeeded){
			
			Site curNeighSite = gameMap.getSite(currentsNeighbor);
			int strAdded = (excessStr[curNeighIndex] + (roundToCheck - distance[curIndex] - prevMoves[curNeighIndex])*curNeighSite.production);
			//writer << "  StrAdded: " << strAdded << endl;
			
			strengthFound += strAdded;
			newExcessStr[curNeighIndex] = 0;
			newMoves[curNeighIndex] = roundToCheck - distance[curIndex] + 1;
			newDirs[curNeighIndex] = oppositeDirection(dir);
			
		      }
		      
		      
		      if(strengthFound > strengthNeeded){
			
			//writer << "Have the str we need! " << endl;
			if(strengthFound > 255)
			  strengthFound = 255;
			
			newExcessStr[neighIndex] = strengthFound - strengthNeeded;
			haveSolution = true;
			
		      }
		      
		      
		    }//End if currentsNeighbor is valid
		    
		    
		    
		  }//End dir
		  
		  //writer << "Before queue erase" << endl;
		  //queue.erase(queue.begin());
		  //writer << "After queue erase" << endl;
		  
		}//End while(queue not empty)
		
		
		if(!haveSolution){
		  //writer << "No solution.  Going back." << endl;
		  roundToCheck++;
		}
		else{
		  
		  //writer << "Storing solution info." << endl;
		  int z = newSoln.size()-2;
		  while(z>=0 && newSoln[z] > newSoln[z+1]){
		    
		    int tmp = newSoln[z+1];
		    newSoln[z+1] = newSoln[z];
		    newSoln[z] = tmp;
		    
		    z--;
		    
		  }
		  
		  //writer << "Extending to in round " << roundToCheck << ":" << endl;
		  //for(int z = 0; z<newSoln.size(); z++) writer << "(" << newSoln[z]%gameMap.width << ", " << newSoln[z]/gameMap.width << "), ";
		  //writer << endl;
		  
		  //writer << "Sorted sol." << endl;
		  
		  if(optRounds[newSoln.size()-startingOffset-1].find(newSoln) == optRounds[newSoln.size()-startingOffset-1].end() || optRounds[newSoln.size()-startingOffset-1][newSoln] > roundToCheck){
		    
		   // writer << "First time seeing or better than previous" << endl;
		    
		    unordered_map<vector<int>,int, solutionHash<vector<int> > >::const_iterator got = optRounds[newSoln.size()-startingOffset-1].find(newSoln);
		    if(got != optRounds[newSoln.size()-startingOffset-1].end()){
		      
		      //writer << "Taking out worse order." << endl;
		      
		      int oldRound = optRounds[newSoln.size()-startingOffset-1][newSoln];
		      
		      
		      auto it = find(solsByRound[oldRound].begin(), solsByRound[oldRound].end(), newSoln);
		      if(it != solsByRound[oldRound].end()){
			
			swap(*it, solsByRound[oldRound].back());
			solsByRound[oldRound].pop_back();
		      }
		      
		      //optRounds[newSoln.size()-startingOffset-1].erase(got);
		      
		    }
		    else{
		      //writer << "First time seeing" << endl; 
		    }
		    
		    //writer << "trying to add stuff" << endl;
		    
		    solsByRound[roundToCheck].push_back(newSoln);
		    
		    optMoves[newSoln.size()-startingOffset-1][newSoln] = newMoves;
		    optDirs[newSoln.size()-startingOffset-1][newSoln] = newDirs;
		    
		    int newProd = prevProd+neighborSite.production;
		    optProds[newSoln.size()-startingOffset-1][newSoln] = newProd;
		    
		    optRounds[newSoln.size()-startingOffset-1][newSoln] = roundToCheck;
		    optSquareToTake[newSoln.size()-startingOffset-1][newSoln] = neighIndex;
		    optExcessStr[newSoln.size()-startingOffset-1][newSoln] = newExcessStr;
		    
		    int optFor = roundToCheck;
		    
		    //writer << "Added stuff." << endl;
		    
		    while(optFor <= endOfGame && optProdPerRound[optFor] <= newProd){
		     optProdPerRound[optFor] = newProd;
		     optSolPerRound[optFor] = newSoln;
		     optFor++;
		    }
		    
		    //writer << "Updated optFor." << endl;
		    
		    
		    
		  }
		  else{
		    
		   //writer << "There was already an order that was better than us." << endl; 
		  }
		  
		  
		  
		  
		}
		
		
		
	      }//End while(!haveSolution)
	      
	      
	      
	    }//End if we haven't checked this square 
	    
	  }//end of neighbor check for prevSolution[j]
	  
	  
	  
	  
	}//End of extending solution.
	
	
	
      }//End if solution is big enough compared to opt
      
      
      //Check time.  Return if out of time.
      get_time::time_point timeStamp = get_time::now();
      auto diff = timeStamp - startTime;
      
      if(chrono::duration_cast<ms>(diff).count() > maxtime*1000 - 200){
	//writer << "Out of time.  Peacing out!" << endl;
	stillHaveTime = false;
	return;
      }
      else{
	//writer << "Still have time." << endl;
      }
      
    }//End solsByRound[roundToCompute].size() > 0
    
    roundToCompute++;
    
  }//End of main loop
  
  
}



Location getNeighborLoc(Location loc, unsigned char d){
  //1=N, 2=E, 3=S, 4=W
  
  //writer.print("getNeighborLoc"); writer.flush();
  unsigned short neighborX = loc.x, neighborY = loc.y;
  
  if(d == EAST){
    neighborX++;
    
    if(neighborX >= gameMap.width) neighborX = 0;
  }
  else if(d == WEST){
    neighborX--;
    
    if(neighborX >= gameMap.width) neighborX = gameMap.width-1;
  }
  else if(d == NORTH){
    neighborY--;
    
    if(neighborY>=gameMap.height) neighborY = gameMap.height-1;
  }
  else{
    neighborY++;
    
    if(neighborY>=gameMap.height) neighborY = 0;
  }
  
  //writer.print("  Returning: (" + neighborX + ", " + neighborY + ").  "); writer.flush();
  return { neighborX,neighborY };
  
}



unsigned char oppositeDirection(unsigned char dir){
  //1=N, 2=E, 3=S, 4=W
  
  if(dir == NORTH)
    return SOUTH;
  
  if(dir==SOUTH)
    return NORTH;
  
  if(dir==EAST)
    return WEST;
  
  
  return EAST;
  
}



















