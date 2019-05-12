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
                    List<Float> probs = new ArrayList<Float>();
                    Integer[] arr = list.toArray(new Integer[list.size()]);
                    Random rand = new Random();

                    float bestScore = -1000;
                    int bestId = i;
                    int units = tiles.get(i).myUnits;
                    next.add(i);
                    if(tiles.get(i).myUnits > 10){
                        next.add(list.get(0));
                    }

                    probs.add(tiles.get(i).charge);
                    for(int l=0; l < list.size(); l++){
                        if (tiles.get(list.get(l)).ownerId != myId) {
                            if(tiles.get(list.get(l)).enemyUnits > 1 && !tiles.get(list.get(l)).enemyHQ){
                                for(int p=0; p < Math.min(4, tiles.get(list.get(l)).enemyUnits); p++){
                                    next.add(i);
                                }
                            }
                            next.add(arr[l]);
                        }
                        if (tiles.get(list.get(l)).total_score > bestScore) {
                            if(tiles.get(i).myUnits > 10){
                                next.set(1, next.get(0));
                            }
                            bestScore = tiles.get(list.get(l)).total_score;
                            bestId = list.get(l);
                            next.set(0, arr[l]);
                        } else if (tiles.get(list.get(l)).total_score > bestScore - 10) {
                            next.add(arr[l]);
                        }

                    }
                    for(int k = 0; k < tiles.get(i).myUnits; k++){
                        int j;
                        if(tiles.get(i).myUnits == 1 && next.size() > 1){
                            j = next.get(rand.nextInt(next.size() - 1) + 1);
                        }
                        else if(k > next.size() - 1){
                            if(k % 10 == 8){
                                j = next.get(0);
                            } else {
                                j = next.get(0);
                            }
                        } else {

                            j = next.get(k);
                        }
                        //System.err.println(k + " " + j + " " + tiles.get(j).total_score);
                        if(i != j){

                            order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
                        }
                    }
                }
            }

            System.out.println(order);
            System.out.println("WAIT");
        }
    }

    public static void updateScores(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        for(int i = 0; i < zoneCount; i++){
            spreadField(tiles, i, myId, 25, tiles.get(i).charge);
            for(int j = 0; j < zoneCount; j++){
                tiles.get(j).total_score += tiles.get(j).current_score;
                tiles.get(j).current_score = 0;
            }
        }

    }

    public static void spreadField(HashMap<Integer, Tile> tiles, int zId, int myId, int depth, float curr_charge){
        if(depth == 0){
            return;
        }
        List<Integer> near = tiles.get(zId).linkedTiles;
        float spread = curr_charge * 0.8f;
        for(int i = 0; i < near.size(); i++){
            if(tiles.get(near.get(i)).current_score < spread){
                tiles.get(near.get(i)).current_score = spread;
                spreadField(tiles, near.get(i), myId, depth - 1, spread);
            }
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
        List<Integer> change = new ArrayList<Integer>();
        for(int i = 0; i < zoneCount; i++){
            if(tiles.get(i).visible == 0){
                List<Integer> near = tiles.get(i).linkedTiles;
                for(int j = 0; j < near.size(); j++){
                    if(tiles.get(near.get(j)).ownerId != myId && tiles.get(near.get(j)).ownerId != -1){
                        change.add(i);
                    }
                }
            }
        }
        for(int i = 0; i < change.size(); i++){
            tiles.get(change.get(i)).ownerId = enemyId;
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
    float current_score = 0;
    float total_score = 0;


    public Tile(int id, int platinumSource, int myId){
        this.id = id;
        this.platinumSource = platinumSource;
        this.myId = myId;
    }

    public static int getDistance(int z1, int z2,HashMap<Integer, Tile> tiles){
        HashMap<Integer, Integer> distances = new HashMap<Integer, Integer>();
        for (int i : tiles.keySet()) {
            distances.put(i,1000);
        }
        distances.put(z1,0);
        for (int i : tiles.keySet()) {
            for (int j : tiles.keySet()) {
                int min=distances.get(j);
                for (int k : tiles.get(j).linkedTiles) {
                    if ((distances.get(k)+1)<min) {
                        min=distances.get(k)+1;
                    }
                }
                distances.put(j,min);
            }
        }
        return distances.get(z2);
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
        this.current_score = 0;
        this.total_score = 0;
    }

    private float initialCharge(){
        float sum = 0;
        if(this.myHQ){
            sum -= 200;
        } else if(this.enemyHQ){
            sum += 1000;
        }

        sum -= 0 * this.myUnits;
        sum += 10 * this.enemyUnits;

        if(this.ownerId == -1){
            sum += 25;
            sum += 50 * this.platinumSource;
        } else if(this.ownerId == this.myId){
            sum -= 100;
        } else {
            sum += 200;
            sum += 20 * this.platinumSource;
        }



        return sum;
    }


}