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
            tiles.put(zoneId,new Tile(zoneId,platinumSource));
            System.err.println(zoneId);
        }
        System.err.println("___________________________");
        for (int i = 0; i < linkCount; i++) {
            int zone1 = in.nextInt();
            int zone2 = in.nextInt();
            System.err.println(zone1);
            tiles.get(zone1).addLinkedTile(zone2);
            tiles.get(zone2).addLinkedTile(zone1);
        }

        // game loop
        while (true) {
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

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");


            // first line for movement commands, second line no longer used (see the protocol in the statement for details)
            
            System.out.println("WAIT");
            System.out.println("WAIT");
        }
    }
}

class Tile {
    public int id;
    public int platinumSource;
    public ArrayList<Integer> linkedTiles = new ArrayList<Integer>();
    int ownerId;
    int myUnits;
    int enemyUnits;
    int visible;
    
    
    public Tile(int id, int platinumSource){
        this.id=id;
        this.platinumSource=platinumSource;
        
    }
    
    public void addLinkedTile(int tile) {
        this.linkedTiles.add(tile);
    }
    
    public void update(int ownerId, int myUnits, int enemyUnits, int visible, int platinum)
    {
        this.ownerId=ownerId;
        this.myUnits=myUnits;
        this.enemyUnits=enemyUnits;
        this.visible=visible;
        this.platinumSource=platinum;
    }
}
