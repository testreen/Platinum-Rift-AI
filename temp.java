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

                // Do turn 1 stuff (find headquarters)
                if(turn == 1){
                    if(ownerId == myId){
                        tiles.get(i).myHQ = true;
                    } else if (ownerId != -1){
                        tiles.get(i).enemyHQ = true;
                        for(int j = 0; j < zoneCount; j++) {
                            tiles.get(j).distance_enemyHQ = tiles.get(i).getDistance(i, j, tiles);
                        }
                    }
                }

                if(myId==1) {
                    tiles.get(zId).update(ownerId,podsP1,podsP0,visible,platinum);
                }
                else {
                    tiles.get(zId).update(ownerId,podsP0,podsP1,visible,platinum);
                }

            }

            // Expand enemy towards not visible areas
            expandOwn(tiles, zoneCount, myId);
            expandEnemy(tiles, zoneCount, myId);




            // Calculate field values spread over other tiles
            updateFields(tiles, zoneCount, myId);

            // Move PODs
            for(int i=0;i<zoneCount;i++){
                if(tiles.get(i).myUnits>0){
                    List<Integer> list = tiles.get(i).linkedTiles; // neighbours in list
                    List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
                    Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
                    Random rand = new Random();

                    // initialize lists and best score
                    float bestScore = tiles.get(i).total_score;
                    int bestDistance = 1000;
                    int bestDistanceId = i;
                    int bestId = i;
                    int units = tiles.get(i).myUnits;
                    next.add(i);
                    // if > 10 units, save 2nd best tile too
                    if(tiles.get(i).myUnits > 10){
                        next.add(i);
                    }

                    for(int l=0; l < list.size(); l++){
                        //System.err.println(list.get(l) + ": " + tiles.get(list.get(l)).total_score + " : " + tiles.get(list.get(l)).chargeTiles );
                        // if neighbour is not under our control
                        if (tiles.get(list.get(l)).ownerId != myId) {
                            // if enemy unit is close, leave up to 4 units to avoid getting passed
                            if(tiles.get(list.get(l)).enemyUnits > 1 && (!tiles.get(list.get(l)).enemyHQ || tiles.get(i).myHQ)){
                                for(int p=0; p < Math.min(4, tiles.get(list.get(l)).enemyUnits); p++){
                                    next.add(i);
                                }
                            } else if(tiles.get(list.get(l)).enemyUnits == 1){
                                next.add(i);
                            }
                            // add not controlled area to list of moves
                            next.add(arr[l]);
                        }

                        // if new best tile to move to
                        if (tiles.get(list.get(l)).total_score > bestScore) {
                            // if over 10 units, also save a 2nd best
                            if(tiles.get(i).myUnits > 10){
                                next.set(1, next.get(0));
                            }
                            bestScore = tiles.get(list.get(l)).total_score;
                            bestId = list.get(l);
                            next.set(0, arr[l]);
                        }

                        if (tiles.get(list.get(l)).distance_enemyHQ < bestDistance){
                            bestDistance = tiles.get(list.get(l)).distance_enemyHQ;
                            bestDistanceId = list.get(l);
                        }
                    }
                    System.err.println(i + ": " + bestScore);
                    if(bestScore < 0){
                        next.set(0, bestDistanceId);
                    }
                    // assign unit moves
                    for(int k = 0; k < tiles.get(i).myUnits; k++){
                        int j;
                        // if only one unit in tile and has neighbors not under our control,
                        // move to one of them randomly
                        if(tiles.get(i).myUnits == 1 && next.size() > 1){
                            j = next.get(rand.nextInt(next.size() - 1) + 1);
                        }

                        // if no more forced moves, move to best tile
                        else if(k > next.size() - 1){
                            j = next.get(0);

                            // if forced move
                        } else if(tiles.get(i).myUnits > 1 && next.size() > 1){
                            j = next.get(k);
                        } else {
                            j = next.get(0);
                        }
                        // to avoid error if staying in same tile
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

    public static void updateFields(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        float DECAY_TILES = 0.75f;

        for(int i = 0; i < zoneCount; i++){
            Tile currTile = tiles.get(i);
            currTile.fieldTiles = new Field(tiles, currTile.chargeTiles, DECAY_TILES, i, myId, 5);

            // add charges from field to total scores and reset values
            for(int j = 0; j < zoneCount; j++){
                if(currTile.fieldTiles.charges.containsKey(j)){
                    tiles.get(j).total_score += currTile.fieldTiles.charges.get(j);
                }
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
                // if not visible tile has enemy neighbour, change to enemy controlled
                for(int j = 0; j < near.size(); j++){
                    if(tiles.get(near.get(j)).visible == 0 && tiles.get(near.get(j)).enemyUnits > 0){
                        change.add(i);
                    }
                }
            }
        }
        for(int i = 0; i < change.size(); i++){
            tiles.get(change.get(i)).ownerId = enemyId;
            tiles.get(change.get(i)).enemyUnits = 2;
        }
    }

    public static void expandOwn(HashMap<Integer, Tile> tiles, int zoneCount, int myId){

        List<Integer> change = new ArrayList<Integer>();
        for(int i = 0; i < zoneCount; i++){
            if(tiles.get(i).visible == 0){
                List<Integer> near = tiles.get(i).linkedTiles;
                // if not visible tile has enemy neighbour, change to enemy controlled
                for(int j = 0; j < near.size(); j++){
                    if(tiles.get(near.get(j)).visible == 0 && tiles.get(near.get(j)).ownerId == myId){
                        change.add(i);
                    }
                }
            }
        }

        for(int i = 0; i < change.size(); i++){
            if(Math.random()  > 0.5){
                tiles.get(change.get(i)).ownerId = myId;
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
    boolean enemyHQ = false;
    boolean myHQ = false;
    float total_score = 0;
    int distance_enemyHQ = 1000;
    float chargeTiles = 0;
    HashMap<Integer, Integer> distances;

    public Field fieldTiles;


    public Tile(int id, int platinumSource, int myId){
        this.id = id;
        this.platinumSource = platinumSource;
        this.myId = myId;
        this.distances = new HashMap<Integer, Integer>();
    }

    public int getDistance(int z1, int z2,HashMap<Integer, Tile> tiles){
        if(!this.distances.containsKey(z1)){
            this.distances.put(z1,0);
        }

        for (int i : tiles.keySet()) {
            if(!this.distances.containsKey(i)){
                this.distances.put(i,100);
            }
        }



        for (int i = 0; i < 30; i++) {
            for (int j : tiles.keySet()) {
                int min=this.distances.get(j);
                for (int k : tiles.get(j).linkedTiles) {
                    if ((this.distances.get(k)+1)<min) {
                        min = this.distances.get(k)+1;
                    }
                }
                this.distances.put(j,min);
            }
        }
        return this.distances.get(z2);
    }


    public void addLinkedTile(int tile) {
        this.linkedTiles.add(tile);
    }

    public void update(int ownerId, int myUnits, int enemyUnits, int visible, int platinum)
    {
        if(visible == 1){
            if(ownerId != this.myId && ownerId != -1){
                this.ownerId = ownerId;
            } else if(ownerId == this.myId || ownerId == -1) {
                this.ownerId = ownerId;
            }
            this.enemyUnits = enemyUnits;
            this.platinumSource = platinum;
        }
        this.myUnits = myUnits;
        this.visible = visible;
        this.total_score = 0;
        this.chargeTiles = 0;
        // Reset charge to only be affected by own values
        updateCharge();
    }

    private void updateCharge(){
        this.chargeTiles = 0;
        if(this.myHQ){
            this.chargeTiles -= 10;
        } else if(this.enemyHQ){
            this.chargeTiles += 100;
        }

        this.chargeTiles -= 0f * this.myUnits;
        this.chargeTiles += 2f * this.enemyUnits;

        if(this.ownerId == -1){
            this.chargeTiles += 14f;
            this.chargeTiles += 5f * this.platinumSource;
        } else if(this.ownerId == this.myId){
            this.chargeTiles -= 5f;
        } else {
            this.chargeTiles += 15f;
            this.chargeTiles += 5f * this.platinumSource;
        }
        this.chargeTiles -= this.distance_enemyHQ / 10;
        this.total_score += this.chargeTiles;

    }
}

class Field {
    float decay = 1f;
    int myId = 0;
    HashMap<Integer, Tile> tiles = new HashMap<Integer, Tile>();
    public HashMap<Integer, Float> charges = new HashMap<Integer, Float>();

    public Field(HashMap<Integer, Tile> tiles, float charge, float decay, int zId, int myId, int range){
        this.tiles = tiles;
        this.decay = decay;
        this.myId = myId;
        charges.put(zId, charge);
        this.spread(tiles, zId, charge, myId, range);
    }

    public void spread(HashMap<Integer, Tile> tiles, int id, float charge, int myId, int range){
        if(range == 0){
            return;
        }
        List<Integer> near = tiles.get(id).linkedTiles;
        for(int i = 0; i < near.size(); i++){
            if(charges.containsKey(tiles.get(near.get(i)))){
                if(charges.get(near.get(i)) < charge * this.decay){
                    charges.replace(near.get(i), charge * this.decay);
                    this.spread(tiles, near.get(i), charge * this.decay, myId, range - 1);
                }
            } else {
                charges.put(near.get(i), charge * this.decay);
                this.spread(tiles, near.get(i), charge * this.decay, myId, range - 1);
            }
        }
    }
}