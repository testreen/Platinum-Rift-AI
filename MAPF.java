import java.util.*;
import java.io.*;
import java.math.*;
import java.util.HashMap; // import the HashMap class
import java.util.ArrayList; // import the ArrayList class



/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    public static void main(String args[]) {

        HashMap<Integer, Tile> tiles = new HashMap<Integer, Tile>();
        Scanner in = new Scanner(System.in);
        int playerCount = in.nextInt(); // the amount of players (always 2)
        int myId = in.nextInt(); // my player ID (0 or 1)
        int zoneCount = in.nextInt(); // the amount of zones on the map
        int linkCount = in.nextInt(); // the amount of links between all zones
        for (int i = 0; i < zoneCount; i++) {
            int zoneId = in.nextInt(); // this zone's ID (between 0 and zoneCount-1)
            int platinumSource = in.nextInt(); // Because of the fog, will always be 0
            tiles.put(zoneId, new Tile(zoneId,platinumSource));
        }
        System.err.println("___________________________");
        for (int i = 0; i < linkCount; i++) {
            int zone1 = in.nextInt();
            int zone2 = in.nextInt();
            tiles.get(zone1).addLinkedTile(zone2);
            tiles.get(zone2).addLinkedTile(zone1);
        }

        // game loop
        int turn = 0
        while (true) {
            turn++;
            int myPlatinum = in.nextInt(); // your available Platinum
            for (int i = 0; i < zoneCount; i++) {
                int zId = in.nextInt(); // this zone's ID
                int ownerId = in.nextInt(); // the player who owns this zone (-1 otherwise)
                int podsP0 = in.nextInt(); // player 0's PODs on this zone
                int podsP1 = in.nextInt(); // player 1's PODs on this zone
                int visible = in.nextInt(); // 1 if one of your units can see this tile, else 0
                int platinum = in.nextInt(); // the amount of Platinum this zone can provide (0 if hidden by fog)
                if(myId==1)
                {
                    tiles.get(zId).update(ownerId,podsP1,podsP0,visible,platinum);
                }
                else
                {
                    tiles.get(zId).update(ownerId,podsP0,podsP1,visible,platinum);
                }

            }

        }
        int started = false;
        if(!started) {
            for(int i = 0; i < zoneCount; i++){
                List<Integer> list = tiles.get(i).linkedTiles;
                boolean enemyStart = true;
                for(int j = 0; j < list.size(); j++){
                    if(tiles.get(i).ownerId == myId || tiles.get(list.get(j)).ownerId == tiles.get(i).ownerId){
                        enemyStart = false;
                    }
                }
            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            String order = "";
            updateScores(tiles, zoneCount, myId);
            for(int i=0;i<zoneCount;i++) {
                //System.err.println("i: " + i + " score: " + tiles.get(i).score);
                if(tiles.get(i).myUnits>0)
                {
                    for(int k=0; k<tiles.get(i).myUnits; k++)
                    {
                        List<Integer> list = tiles.get(i).linkedTiles;
                        List<Integer> next = new ArrayList<Integer>();
                        Integer[] arr = list.toArray(new Integer[list.size()]);
                        Random rand = new Random();
                        int j = arr[rand.nextInt(list.size())];
                        int bestScore = tiles.get(i).score;
                        int bestId = i;
                        next.add(i);

                        // Prioritize moving to not already owned cells nearby
                        for(int l=0; l < list.size(); l++){
                            //System.err.println(list.get(l));

                            if(tiles.get(list.get(l)).score > bestScore){
                                bestScore = tiles.get(list.get(l)).score;
                                bestId = list.get(l);
                                next = new ArrayList<Integer>();
                                next.add(arr[l]);
                            } else if(tiles.get(list.get(l)).score == bestScore){
                                next.add(arr[l]);
                            }

                        }
                        if(next.size() > 0){
                            j = next.get(rand.nextInt(next.size()));
                            //j = next.get(0);
                        }

                        //int[] list=tiles.get(i).linkedTiles.toArray(new Integer[tiles.get(i).linkedTiles.size()]);
                        if(tiles.get(j).enemyUnits > 0 && tiles.get(i).myUnits < tiles.get(j).enemyUnits) {
                            if(rand.nextInt(tiles.get(i).myUnits) > 1){
                                order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
                            }
                        } else {
                            order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
                        }
                    }
                }
            }
            // first line for movement commands, second line no longer used (see the protocol in the statement for details)

            System.out.println(order);
            System.out.println("WAIT");
        }
    }

    public static void updateScores(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        for(int i = 0; i < zoneCount; i++){
            List<Integer> near = tiles.get(i).linkedTiles;
            boolean frontline = false;
            for(int j = 0; j < near.size(); j++){
                if(tiles.get(i).ownerId != myId && tiles.get(near.get(j)).ownerId == myId){
                    frontline = true;
                }
            }
            if(frontline){
                if(tiles.get(i).ownerId != myId){
                    tiles.get(i).frontline = true;
                }
                if(tiles.get(i).ownerId == -1){
                    tiles.get(i).score += 50;
                } else {
                    tiles.get(i).score += 50; // Change to score based on distance to headquarters?
                }
            } else {
                tiles.get(i).frontline = false;
            }
        }

        for(int i = 0; i < zoneCount; i++){

            if(tiles.get(i).frontline && tiles.get(i).ownerId != myId){
                spreadFront(tiles, i, tiles.get(i).score, myId);
            }
        }
    }

    public static void spreadFront(HashMap<Integer, Tile> tiles, int id, int score, int myId){
        List<Integer> near = tiles.get(id).linkedTiles;
        for(int i = 0; i < near.size(); i++){
            //System.err.println("id: " + id);
            //System.err.println("score: " + score);
            //System.err.println("near id: " + tiles.get(near.get(i)).id);
            //System.err.println("near score: " + tiles.get(near.get(i)).score);
            if(tiles.get(near.get(i)).score < score && tiles.get(near.get(i)).ownerId == myId){
                tiles.get(near.get(i)).score = score - 1;
                spreadFront(tiles, near.get(i), score - 1, myId);
            }
        }
    }

    public static void spreadStart(
}

// 1) Identifying the objects
//        - Our own occupied tiles
//        - Unoccupied tiles
//        - Tiles occupied by enemy
//        - Our PODs
//        - Enemy PODs
//        - Platinum
//        - Our Headquarter
//        - Enemy Headquarter

class Tile {
    public int id;
    public int platinumSource;
    public ArrayList<Integer> linkedTiles = new ArrayList<Integer>();
    int ownerId = -1;
    int myUnits;
    int enemyUnits;
    int visible;
    int score = 0;
    boolean frontline = false;
    int startScore = 0;


    public Tile(int id, int platinumSource){
        this.id=id;
        this.platinumSource=platinumSource;

    }

    public void addLinkedTile(int tile) {
        this.linkedTiles.add(tile);
    }

    public void update(int ownerId, int myUnits, int enemyUnits, int visible, int platinum)
    {
        if(visible == 1){
            this.ownerId=ownerId;
            this.enemyUnits=enemyUnits;
            this.platinumSource=platinum;
        }
        this.myUnits=myUnits;
        this.visible=visible;

        this.score = startScore;
        this.frontline = false;
    }
}