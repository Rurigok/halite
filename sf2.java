




import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;


public class sf2 {

    static int width;
    static int height;
    static int eMax;
    static int myID;
    static GameMap gameMap;

    static double[][] pGrid;
    static boolean[][] eGrid;
    static int[][] nGrid;
    static int[][] dGrid;
    static int[][] needGrid;
    static int[][] turnsGrid;
    static boolean[][] foundGrid;
    static boolean largeMove = false;
    static FileWriter out;
    static ArrayList<double[][]> history = new ArrayList<double[][]>();



    /** CONSTANTS */

    static int blurDis = 1;
    static final double minFlow = .005;
    static final double enemyMult = 4;
    static final double waitNTurns = 2.5;
    static int blurPasses = 5;
    static final double strengthPower = 6;
    static final double productionWeight = 1;
    static final int collideThresh = 280;
    static final double lowerWeight = .99;
    static final int attackDis = 2;

    /*
    static int blurDis = 1;
    static final double minFlow = .005;
    static final double enemyMult = 4;
    static final double waitNTurns = 2.5;
    static int blurPasses = 5;
    static final double strengthPower = 2;
    static final double productionWeight = 1;
    static final int collideThresh = 280;

    ###### Final Scores (0-100 scale) ####### 200 games
    sf2: 55
    MyBot: 46
    stux: 48
     */



    /***/

    public static void main(String[] args) throws java.io.IOException {
        InitPackage iPackage = Networking.getInit();
        myID = iPackage.myID;
        gameMap = iPackage.map;

        height = gameMap.height;
        width = gameMap.width;

        pGrid = new double[width][height];
        eGrid = new boolean[width][height];
        nGrid = new int[width][height];
        dGrid = new int[width][height];
        needGrid = new int[width][height];
        turnsGrid = new int[width][height];
        foundGrid = new boolean[width][height];

        //out = new FileWriter("out.json");

        eMax = 1;
//        if (Math.sqrt(height*width) <= 35) {
//            blurDis = 1;
//            blurPasses = 1;
//        }







        Networking.sendInit("RStayFlow");

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();



            gameMap = Networking.getFrame();
            pGrid = new double[width][height];
            nGrid = new int[width][height];
            dGrid = new int[width][height];
            needGrid = new int[width][height];
            turnsGrid = new int[width][height];
            foundGrid = new boolean[width][height];

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));

                    if(site.owner == myID) {

                        continue;
                    }
                    else {
                        if (site.owner == 0) {

                            pGrid[x][y] = site.production / 15.0 * productionWeight + (Math.pow(1-(site.strength / 255.0), strengthPower));
                            if (site.production == 0)
                                pGrid[x][y] = 0;
                        }
                        else
                            pGrid[x][y] = site.production/15.0 * enemyMult;
                    }
                }
            }


            for (int i = 0; i < blurPasses; i++)
                for(int y = 0; y < gameMap.height; y++)
                    for(int x = 0; x < gameMap.width; x++)
                        pGrid[x][y] = smoothCell(x, y, blurDis);

            for(int y = 0; y < gameMap.height; y++)
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    if (site.owner == myID) {
                        while (isMax(x, y)) {
                            pGrid[x][y] *= lowerWeight;
                        }
                        if (enemyAttackableEdge(x, y)) {
                            emptyArea(x, y, attackDis);
                        }
                    }
                }



            //writeGrid(pGrid);

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));

                    if(site.owner == myID) {

                        moves.add(new Move(new Location(x, y), Direction.DIRECTIONS[move(x, y)]));
                    }

                }
            }
            Networking.sendFrame(moves);
        }
    }

    public ArrayList<int[]> getEdges() {

        ArrayList<int[]> out = new ArrayList<int[]>();
        for(int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                int[] point = {x, y};
                if (isEdge(x, y)) out.add(point);
            }
        }
        return out;
    }

    public static boolean isEdge(int x, int y) {
        int[][] locs = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        for (int i = 1; i < locs.length; i++) {
            int tx = locs[i][0];
            int ty = locs[i][1];

            Site site = gameMap.getSite(new Location(tx, ty));
            if (site.owner != myID) {
                return true;
            }
        }
        return false;
    }

    public static boolean enemyAttackableEdge(int x, int y) {
        int[][] locs = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        for (int i = 1; i < locs.length; i++) {
            int tx = locs[i][0];
            int ty = locs[i][1];

            Site site = gameMap.getSite(new Location(tx, ty));
            if (site.strength == 0 && enemyNear(tx, ty)) {
                return true;
            }
        }
        return false;
    }

    public static boolean enemyNear(int x, int y) {
        int[][] locs = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        for (int i = 0; i < locs.length; i++) {
            int tx = locs[i][0];
            int ty = locs[i][1];

            Site site = gameMap.getSite(new Location(tx, ty));
            if (site.owner > 0 && site.owner != myID)
                return true;
        }
        return false;
    }

    public static void emptyArea(int x, int y, int dis) {




        for (int dy = -dis; dy <= dis; dy++) {
            for (int dx = -dis; dx <= dis; dx++) {
                int tx = X(dx + x);
                int ty = Y(y + dy);

                Site site = gameMap.getSite(new Location(tx, ty));

                if (site.owner == 0 && site.strength > 1)
                    pGrid[tx][ty] = 0.0;
            }
        }


    }

    public static boolean isMax(int x, int y) {
        x = X(x);
        y = Y(y);

        int[][] locs = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        double max = 0;

        for (int i = 0; i < locs.length; i++) {
            int tx = locs[i][0];
            int ty = locs[i][1];

            if (i == 0) {
                max = pGrid[tx][ty];
                continue;
            }

            if (pGrid[tx][ty] > max)
                return false;
        }
        return true;

    }

    public static int X(int i) {
        if (i >= width)
            i -= width;
        else if (i < 0)
            i += width;
        return i;
    }

    public static int Y(int i) {
        if (i >= height)
            i -= height;
        else if (i < 0)
            i += height;
        return i;
    }

    public static int[] direct(int x, int y, int d) {

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        return locations[d];
    }

    public static Site directSite(int x, int y, int d) {
        int[] temp = direct(x, y, d);

        Site t = gameMap.getSite(new Location(temp[0], temp[1]));
        return t;
    }

    public static boolean onEdge(int x, int y) {
        x = X(x);
        y = Y(y);

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        for (int[] i : locations) {
            int tx = i[0];
            int ty = i[1];
            Site site = gameMap.getSite(new Location(tx, ty));

            if (site.owner != myID)
                return true;
        }
        return false;
    }

    public static boolean maxArea(int x, int y) {
        x = X(x);
        y = Y(y);

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};
        double maxP = pGrid[x][y];

        for (int[] i : locations) {
            int tx = i[0];
            int ty = i[1];

            if (pGrid[tx][ty] > maxP)
                return false;
        }
        return true;
    }

    public static void lowerArea(int x, int y, int amt) {
        x = X(x);
        y = Y(y);

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};


        for (int[] i : locations) {
            int tx = i[0];
            int ty = i[1];

            pGrid[tx][ty] -= amt;
        }
    }

    public static double smoothCell(int x, int y) {

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};
        double total = 0.0;
        for (int[] i : locations) {
            int tx = i[0];
            int ty = i[1];

            total += pGrid[tx][ty];

        }
        total /= 5;
        return total;

    }

    public static double smoothCell(int x, int y, int dis) {

        double total = 0.0;

        double count = 0.0;
        for (int dy = -dis; dy <= dis; dy++) {
            for (int dx = -dis; dx <= dis; dx++) {
                total += pGrid[X(x+dx)][Y(y+dy)];
                count++;
            }
        }

        return total/count;
    }

//    public static double smoothCell(int x, int y, int dis) {
//        double total = 0.0;
//
//        int count = 0;
//        for (int dy = -dis; dy <= dis; dy++) {
//            for (int dx = -(dis-Math.abs(dy)); dx <= (dis-Math.abs(dy)); dx++) {
//
//
//                    double d = Math.sqrt((dx * dx + dy * dy))+1;
//                    total += pGrid[X(x + dx)][Y(y + dy)]/d;
//                    count++;
//
//            }
//        }
//        return total/count;
//    }

    public static int move(int x, int y, int depth) {
        Site site = gameMap.getSite(new Location(x, y));

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        double maxP = 0;
        int bestDir = 0;
        boolean easy = false;

        double maxF = 0;

        for (int i = 1; i < locations.length; i++) {
            int tx = locations[i][0];
            int ty = locations[i][1];

            Site tsite = gameMap.getSite(new Location(tx, ty));

            if (pGrid[tx][ty] > maxF) {
                if (nGrid[tx][ty] + site.strength < collideThresh) {
                    maxF = pGrid[tx][ty];
                    bestDir = i;
                }
                else {
                    bestDir = dGrid[tx][ty];
                    //bestDir = 0;
                }
            }
        }

//        if (maxF < minFlow && ! largeMove) {
//            return 0;
//        }


        Site tsite = directSite(x, y, bestDir);
        if (site.strength < site.production*waitNTurns || (tsite.strength+1 > site.strength && tsite.owner != myID)) {
            nGrid[x][y] = site.strength;
            dGrid[x][y] = 0;
            if (tsite.owner != myID) {
                int diff = tsite.strength - (site.strength+site.production);
                needGrid[x][y] = diff;
                if (site.production != 0)
                    turnsGrid[x][y] = diff / site.production;
                else turnsGrid[x][y] = 10000;
                foundGrid[x][y] = true;
            }
            foundGrid[x][y] = true;
            return 0;
        }

        int[] temp = direct(x, y, bestDir);
        int tx = temp[0];
        int ty = temp[1];
        nGrid[temp[0]][temp[1]] = site.strength;
        dGrid[temp[0]][temp[1]] = bestDir;

        if (foundGrid[tx][ty] == false && tsite.owner == myID) {
            if (depth > 20) return -1;
            move(tx, ty, depth+1);
        }

        if (needGrid[tx][ty] > site.strength && turnsGrid[tx][ty] >= (needGrid[tx][ty]+1)/(site.production+1)) {
            int need = needGrid[tx][ty] - (site.strength+site.production);
            int turns;
            if (site.production != 0)
                turns = Math.min(need/site.production, turnsGrid[tx][ty]-1);
            else turns = 10000;
            needGrid[x][y] = need;
            turnsGrid[x][y] = turns;
            nGrid[x][y] = site.strength;
            dGrid[x][y] = 0;
            foundGrid[x][y] = true;
            return 0;
        }


        return bestDir;

    }



    public static int move(int x, int y) {
        Site site = gameMap.getSite(new Location(x, y));

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        double maxP = 0;
        int bestDir = 0;
        boolean easy = false;

        double maxF = 0;

        for (int i = 1; i < locations.length; i++) {
            int tx = locations[i][0];
            int ty = locations[i][1];

            Site tsite = gameMap.getSite(new Location(tx, ty));

            if (pGrid[tx][ty] > maxF) {
                if (nGrid[tx][ty] + site.strength < collideThresh) {
                    maxF = pGrid[tx][ty];
                    bestDir = i;
                }
                else {
                    bestDir = dGrid[tx][ty];
                    //bestDir = 0;
                }
            }
        }

//        if (maxF < minFlow && ! largeMove) {
//            return 0;
//        }


        Site tsite = directSite(x, y, bestDir);
        if (site.strength < site.production*waitNTurns || (tsite.strength+1 > site.strength && tsite.owner != myID)) {
            nGrid[x][y] = site.strength;
            dGrid[x][y] = 0;
            if (tsite.owner != myID) {
                int diff = tsite.strength + 1 - (site.strength+site.production);
                needGrid[x][y] = diff;
                if (site.production != 0)
                    turnsGrid[x][y] = diff / site.production;
                else turnsGrid[x][y] = 10000;
                foundGrid[x][y] = true;
            }
            foundGrid[x][y] = true;
            return 0;
        }

        int[] temp = direct(x, y, bestDir);
        int tx = temp[0];
        int ty = temp[1];
        nGrid[temp[0]][temp[1]] = site.strength;
        dGrid[temp[0]][temp[1]] = bestDir;

        if (foundGrid[tx][ty] == false && tsite.owner == myID) {
            move(tx, ty, 0);
        }

        if (needGrid[tx][ty] > site.strength && turnsGrid[tx][ty] >= (needGrid[tx][ty]+1)/(site.production+1)) {
            int need = needGrid[tx][ty] - (site.strength+site.production);
            int turns;
            if (site.production != 0)
                turns = Math.min(need/site.production, turnsGrid[tx][ty]);
            else turns = 10000;
            needGrid[x][y] = need;
            turnsGrid[x][y] = turns;
            nGrid[x][y] = site.strength;
            dGrid[x][y] = 0;
            foundGrid[x][y] = true;
            return 0;
        }


        return bestDir;

    }

    public static int oppositeDir(int d) {
        int[] temp = {0, 3, 4, 1, 2};
        return temp[d];
    }

    public static void writeGrid(double[][] grid) throws java.io.IOException{



        history.add(grid);



        for(int x = 0; x < gameMap.width; x++) {
            for (int y = 0; y < gameMap.height; y++) {

                out.write(Double.toString(grid[x][y]).toCharArray());
                out.write(' ');

            }
            out.write('\n');
        }
        out.write('\n');





        out.flush();







    }

}




















