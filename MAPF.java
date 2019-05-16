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
        int myHQ = -1;
        int enemyHQ = -1;
        while (true) {
            turn++;
            String order = "";

            // Calculate all distances
            if(turn == 1){
                for(int i = 0; i < zoneCount; i++){
                    tiles.get(i).calcDistances(tiles);
                }
            }

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
                        myHQ = i;
                    } else if (ownerId != -1){
                        tiles.get(i).enemyHQ = true;
                        enemyHQ = i;
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
            expandEnemy(tiles, zoneCount, myId);

            // Calculate field values spread over other tiles
            updateFields(tiles, zoneCount, myId);

            // Move PODs
            for(int i=0;i<zoneCount;i++){
                //System.err.println(i + ": " + tiles.get(i).fieldHQ + " : " + tiles.get(i).distances.get(18));
                if(tiles.get(i).myUnits>0){
                    List<Integer> list = tiles.get(i).linkedTiles; // neighbours in list
                    List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
                    Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
                    Random rand = new Random();

                    // initialize lists and best score
                    float bestScore = tiles.get(i).total_score;
                    float bestHQfield = tiles.get(i).fieldHQ;
                    int bestHQ = i;
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
                                //next.add(i);
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

                        if (tiles.get(list.get(l)).fieldHQ > bestHQfield){
                            bestHQfield = tiles.get(list.get(l)).fieldHQ;
                            bestHQ = list.get(l);
                        }


                    }
                    if(bestScore < 0){
                        next.set(0, bestHQ);
                    }
                    if(tiles.get(myHQ).distances.get(enemyHQ) < 5 || (zoneCount < 50 && tiles.get(myHQ).distances.get(enemyHQ) < 7)){
                        next = new ArrayList<Integer>(); // list of moves from tile
                        if(i == myHQ){


                            if(tiles.get(i).myUnits >= 4){
                                for(int p=0; p < 4; p++){
                                    next.add(i);
                                }
                                for(int p=0; p < tiles.get(i).myUnits - 4; p++){
                                    next.add(bestHQ);
                                }
                            } else if(tiles.get(i).myUnits > 1) {
                                for(int p=0; p < tiles.get(i).myUnits - 1; p++){
                                    next.add(bestHQ);
                                }
                                next.add(i);
                            } else {
                                next.add(i);
                            }
                        } else {
                            for(int p=0; p < tiles.get(i).myUnits; p++){
                                next.add(bestHQ);
                            }
                        }
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
        float DECAY_HQ = 0.99f;
        float DECAY_MY_UNITS = 0.9f;
        float DECAY_ENEMY_UNITS = 0.9f;
        float DECAY_PLATINUM = 0.9f;
        float DECAY_OWNER = 0.9f;
        float DECAY_UNEXPLORED = 0.9f;



        for(int i = 0; i < zoneCount; i++){
            Tile currTile = tiles.get(i);

            // add charges from field to total scores and reset values
            for(int j = 0; j < zoneCount; j++){
                int d = currTile.distances.get(j);
                currTile.fieldHQ += DECAY_HQ / (d + 1) * tiles.get(j).chargeHQ;
                currTile.fieldMyUnits += DECAY_MY_UNITS / (d + 1) * tiles.get(j).chargeMyUnits;
                currTile.fieldEnemyUnits += DECAY_ENEMY_UNITS / (d + 1) * tiles.get(j).chargeEnemyUnits;
                currTile.fieldPlatinum += DECAY_PLATINUM / (d + 1) * tiles.get(j).chargePlatinum;
                currTile.fieldOwner += DECAY_OWNER / (d + 1) * tiles.get(j).chargeOwner;
                currTile.fieldUnexplored += DECAY_UNEXPLORED / (d + 1) * tiles.get(j).chargeUnexplored;
            }
            currTile.total_score = currTile.fieldHQ +
                    currTile.fieldMyUnits +
                    currTile.fieldEnemyUnits +
                    currTile.fieldPlatinum +
                    currTile.fieldOwner +
                    currTile.fieldUnexplored;
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
    public HashMap<Integer, Integer> distances = new HashMap<Integer, Integer>();

    int ownerId = -1;
    int myUnits = 0;
    int enemyUnits = 0;
    int visible = 0;

    int myId = 0;
    boolean enemyHQ = false;
    boolean myHQ = false;
    float total_score = 0;

    float chargeHQ = 0;
    float chargeMyUnits = 0;
    float chargeEnemyUnits = 0;
    float chargePlatinum = 0;
    float chargeOwner = 0;
    float chargeUnexplored = 0;

    float fieldHQ = 0;
    float fieldMyUnits = 0;
    float fieldEnemyUnits = 0;
    float fieldPlatinum = 0;
    float fieldOwner = 0;
    float fieldUnexplored = 0;


    public Tile(int id, int platinumSource, int myId){
        this.id = id;
        this.platinumSource = platinumSource;
        this.myId = myId;
    }

    public void calcDistances(HashMap<Integer, Tile> tiles){
        boolean[] visited = new boolean[tiles.size()];

        LinkedList<Integer> queue = new LinkedList<Integer>();

        visited[this.id]=true;
        queue.add(this.id);
        distances.put(this.id, 0);
        while(queue.size() != 0){
            if(distances.size() == tiles.size()){
                return;
            }
            int s = queue.poll();
            int d = distances.get(s);
            ArrayList<Integer> next = tiles.get(s).linkedTiles;
            for(int i : next){
                if(!visited[i]){
                    visited[i] = true;
                    distances.put(i, d + 1);
                    tiles.get(i).distances.put(this.id, d + 1);
                    tiles.get(i).distances.put(s, 1);
                    queue.add(i);
                }
            }
        }

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

        this.fieldHQ = 0;
        this.fieldMyUnits = 0;
        this.fieldEnemyUnits = 0;
        this.fieldPlatinum = 0;
        this.fieldOwner = 0;
        this.fieldUnexplored = 0;

        // Reset fields to only be affected by own values
        updateCharge();
    }

    private void updateCharge(){
        if(this.myHQ){
            this.chargeHQ = 00;
            //this.fieldHQ = -100;
        } else if(this.enemyHQ){
            this.chargeHQ = 100;
            //this.fieldHQ = 100;
        }

        this.chargeMyUnits = 0f * this.myUnits;
        //this.fieldMyUnits = -5f * this.myUnits;

        this.chargeEnemyUnits = 5f * this.enemyUnits;
        //this.fieldEnemyUnits = 5f * this.enemyUnits;

        if(this.ownerId == -1){
            this.chargeOwner = 0f;
            //this.fieldOwner = 0f;

            this.chargeUnexplored = 10f;
            //this.fieldUnexplored = 10f;

            this.chargePlatinum = 5f * this.platinumSource;
            //this.fieldPlatinum = 2f * this.platinumSource;

        } else if(this.ownerId == this.myId){
            this.chargeOwner = 0f;
            //this.fieldOwner = -5f;

            this.chargeUnexplored = 0f;
            //this.fieldUnexplored = 0f;

            this.chargePlatinum = 0f * this.platinumSource;
            //this.fieldPlatinum = 0f * this.platinumSource;

        } else {
            this.chargeOwner = 10f;
            //this.fieldOwner = 10f;

            this.chargeUnexplored = 0f;
            //this.fieldUnexplored = 0f;

            this.chargePlatinum = 5f * this.platinumSource;
            //this.fieldPlatinum = 2f * this.platinumSource;
        }
    }
}
