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

        // Read initial data
        HashMap<Integer, Tile> tiles = new HashMap<Integer, Tile>();
        Scanner in = new Scanner(System.in);
        int playerCount = in.nextInt(); // the amount of players (always 2)
        int myId = in.nextInt(); // my player ID (0 or 1)
        int zoneCount = in.nextInt(); // the amount of zones on the map
        int linkCount = in.nextInt(); // the amount of links between all zones
        for (int i = 0; i < zoneCount; i++) {
            int zoneId = in.nextInt(); // this zone's ID (between 0 and zoneCount-1)
            int platinumSource = in.nextInt(); // Because of the fog, will always be 0
            tiles.put(zoneId, new Tile(zoneId, platinumSource, myId));
        }
        System.err.println("___________________________");
        for (int i = 0; i < linkCount; i++) {
            int zone1 = in.nextInt();
            int zone2 = in.nextInt();
            tiles.get(zone1).addLinkedTile(zone2);
            tiles.get(zone2).addLinkedTile(zone1);
        }

        // Game loop
        int turn = 0;
        while (true) {
            turn++;
            String order = "";

            // Read turn data and update tiles
            int myPlatinum = in.nextInt(); // your available Platinum
            for (int i = 0; i < zoneCount; i++) {
                int zId = in.nextInt(); // this zone's ID
                int ownerId = in.nextInt(); // the player who owns this zone (-1 otherwise)
                int podsP0 = in.nextInt(); // player 0's PODs on this zone
                int podsP1 = in.nextInt(); // player 1's PODs on this zone
                int visible = in.nextInt(); // 1 if one of your units can see this tile, else 0
                int platinum = in.nextInt(); // the amount of Platinum this zone can provide (0 if hidden by fog)

                if(myId==1) {
                    tiles.get(zId).update(ownerId,podsP1,podsP0,visible,platinum);
                }
                else {
                    tiles.get(zId).update(ownerId,podsP0,podsP1,visible,platinum);
                }

            }

            // Do turn 1 stuff (find headquarters)
            if(turn == 1){
                for (int i = 0; i < zoneCount; i++) {
                    if(tiles.get(i).ownerId == myId){
                        tiles.get(i).myHQ = true;
                    } else if (tiles.get(i).ownerId != -1){
                        tiles.get(i).enemyHQ = true;
                    }
                }
            }

            // Expand enemy towards not visible areas
            expandEnemy(tiles, zoneCount, myId);

            // Calculate field values spread over other tiles
            updateScores(tiles, zoneCount, myId);

            // Move PODs
            for(int i=0;i<zoneCount;i++){
                if(tiles.get(i).myUnits>0){
                    List<Integer> list = tiles.get(i).linkedTiles;
                    List<Integer> next = new ArrayList<Integer>();
                    Integer[] arr = list.toArray(new Integer[list.size()]);
                    Random rand = new Random();
                    int j = arr[rand.nextInt(list.size())];
                    next.add(i);
                    float sum = 0;
                    int units = tiles.get(i).myUnits;
                    List<Float> probs = new ArrayList<Float>();
                    probs.add(tiles.get(i).charge);

                    for(int l=0; l < list.size(); l++){
                        next.add(arr[l]);
                        probs.add(tiles.get(arr[l]).charge);
                        sum += tiles.get(arr[l]).charge;
                    }
                    for(int m = 0; m < next.size(); m++){
                        probs.get(m) = (int) Math.round(props.get(m) * tiles.get(i).myUnits / sum);
                        if(probs.get(m) > 0 && units - probs.get(m) > -1) {
                            order += probs.get(m) + " " + Integer.toString(i) + " " + Integer.toString(next.get(m)) + " ";
                        }
                        units -= probs.get(m);
                    }

                }
            }

            System.out.println(order);
            System.out.println("WAIT");
        }
    }

    public static void updateScores(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        for(int i = 0; i < zoneCount; i++){
            spreadField(tiles, i, myId, 2);
        }
    }

    public static void spreadField(HashMap<Integer, Tile> tiles, int zId, int myId, int depth){
        if(depth == 0){
            return;
        }
        List<Integer> near = tiles.get(zId).linkedTiles;
        float spread = tiles.get(zId).charge / 10;
        tiles.get(zId).charge -= spread;
        for(int i = 0; i < near.size(); i++){
            tiles.get(near.get(i)).charge += spread / near.size();
        }
        for(int i = 0; i < near.size(); i++){
            spreadField(tiles, near.get(i), myId, depth - 1);
        }
    }

    // Expand enemy into not visible areas
    public static void expandEnemy(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        int enemyId;
        if(myId == 0){
            enemyId = 1;
        } else {
            enemyId = 0;
        }
        for(int i = 0; i < zoneCount; i++){
            if(tiles.get(i).visible == 0){
                List<Integer> near = tiles.get(i).linkedTiles;
                for(int j = 0; j < near.size(); j++){
                    if(tiles.get(near.get(j)).ownerId != myId && tiles.get(near.get(j)).ownerId != -1){
                        tiles.get(i).ownerId = enemyId;
                    }
                }
            }
        }
    }
}


class Tile {
    public int id;
    public int platinumSource;
    public ArrayList<Integer> linkedTiles = new ArrayList<Integer>();
    int ownerId = -1;
    int myUnits = 0;
    int enemyUnits = 0;
    int visible = 0;
    float charge = 0;
    int myId = 0;
    boolean frontline = false;
    boolean enemyHQ = false;
    boolean myHQ = false;


    public Tile(int id, int platinumSource, int myId){
        this.id = id;
        this.platinumSource = platinumSource;
        this.myId = myId;
    }

    public void addLinkedTile(int tile) {
        this.linkedTiles.add(tile);
    }

    public void update(int ownerId, int myUnits, int enemyUnits, int visible, int platinum)
    {
        if(visible == 1){
            this.ownerId = ownerId;
            this.enemyUnits = enemyUnits;
            this.platinumSource = platinum;
        }
        this.myUnits = myUnits;
        this.visible = visible;

        // Reset charge to only be affected by own values
        this.charge = initialCharge();
    }

    private float initialCharge(){
        float sum = 0;
        if(this.myHQ){
            sum -= 40;
        } else if(this.enemyHQ){
            sum += 40;
        }

        sum += 2 * this.myUnits;
        sum += 5 * this.enemyUnits;

        if(this.ownerId == -1){
            sum += 5;
        } else if(this.ownerId == this.myId){
            sum -= 5;
        } else {
            sum += 10;
        }

        sum += 2 * this.platinumSource;

        return sum;
    }


}