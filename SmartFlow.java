

import java.awt.*;
import java.util.ArrayList;


public class SmartFlow {

    static int width;
    static int height;
    static int eMax;
    static int myID;
    static GameMap gameMap;

    static double[][] pGrid;
    static boolean[][] eGrid;
    static int[][] nGrid;
    static int[][] dGrid;
    static boolean largeMove = false;


    /** CONSTANTS */

    static final int blurDis = 5;
    static final double minFlow = .1;
    static final double enemyMult = 3;
    static final int waitNTurns = 3;
    static final int blurPasses = 1;
    static final double strengthWeight = 50;



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

        eMax = 1;


        Networking.sendInit("SmartFlow");

        while(true) {
            ArrayList<Move> moves = new ArrayList<Move>();
            double rfac = largeMove ? 2 : 1;
            largeMove = Math.random()*rfac > .5;

            gameMap = Networking.getFrame();
            pGrid = new double[width][height];
            nGrid = new int[width][height];
            dGrid = new int[width][height];
            for (int i = 0; i < 5; i++)
                for(int y = 0; y < gameMap.height; y++) {
                    for(int x = 0; x < gameMap.width; x++) {
                        Site site = gameMap.getSite(new Location(x, y));

                        if(site.owner == myID) {

                            continue;
                        }
                        else {
                            if (site.owner == 0)
                                pGrid[x][y] = site.production - (site.strength/255)*15*strengthWeight;
                            else
                                pGrid[x][y] = site.production * enemyMult;
                        }
                    }
                }
            for (int i = 0; i < blurPasses; i++)
                for(int y = 0; y < gameMap.height; y++)
                    for(int x = 0; x < gameMap.width; x++)
                        pGrid[x][y] = smoothCell(x, y, blurDis);

            for(int y = 0; y < gameMap.height; y++) {
                for(int x = 0; x < gameMap.width; x++) {
                    Site site = gameMap.getSite(new Location(x, y));
                    pGrid[x][y] = smoothCell(x, y);
                    if(site.owner == myID) {

                        moves.add(new Move(new Location(x, y), Direction.DIRECTIONS[move(x, y)]));
                    }

                }
            }
            Networking.sendFrame(moves);
        }
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




    public static int move(int x, int y) {
        Site site = gameMap.getSite(new Location(x, y));

        int[][] locations = {{x, y}, {x, Y(y-1)}, {X(x+1), y}, {x, Y(y+1)}, {X(x-1), y}};

        double maxP = 0;
        int bestDir = 0;
        boolean easy = false;

        double maxF = 0;

        for (int i = 0; i < locations.length; i++) {
            int tx = locations[i][0];
            int ty = locations[i][1];
            Site tsite = gameMap.getSite(new Location(tx, ty));

            if (pGrid[tx][ty] > maxF) {
                if (nGrid[tx][ty] + site.strength < 300) {
                    maxF = pGrid[tx][ty];
                    bestDir = i;
                }
                else {
                    bestDir = dGrid[tx][ty];
                }
            }
        }

//        if (maxF < minFlow && ! largeMove) {
//            return 0;
//        }


        Site tsite = directSite(x, y, bestDir);
        if (site.strength < site.production*waitNTurns || (tsite.strength > site.strength && tsite.owner != myID))
            return 0;

        int[] temp = direct(x, y, bestDir);
        nGrid[temp[0]][temp[1]] = site.strength;
        dGrid[temp[0]][temp[1]] = bestDir;

        return bestDir;

    }

}




















