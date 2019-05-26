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
        GameState.tiles=tiles;
        Explore exploreNode = new Explore();
        ExplorePostRush explorePostRushNode = new ExplorePostRush();
        ShouldWeRush shouldWeRushNode = new ShouldWeRush(); 
        PostRush postRushNode = new PostRush(); 
        Rush rushNode = new Rush();
        Node rushTree = new Sequence(new Node[]{shouldWeRushNode,rushNode});
        Node mainTree = new Fallback(new Node[]{
            shouldWeRushNode,
            new Sequence(new Node[]{postRushNode,explorePostRushNode}),
            exploreNode});
        
        Scanner in = new Scanner(System.in);
        int playerCount = in.nextInt(); // the amount of players (always 2)
        int myId = in.nextInt(); // my player ID (0 or 1)
        GameState.myId=myId;
        int zoneCount = in.nextInt(); // the amount of zones on the map
        GameState.zoneCount=zoneCount;
        Unit.max_nb_explorer=(int) (zoneCount/40);
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
                        GameState.myHQ=myHQ;
                        tiles.get(i).role=Unit.NoRole;
                    } else if (ownerId != -1){
                        tiles.get(i).enemyHQ = true;
                        enemyHQ = i;
                        GameState.enemyHQ=enemyHQ;
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

            
            rushTree.run(tiles.get(myHQ));
            
            // Move PODs
            for(int i=0;i<zoneCount;i++){
                //System.err.println(i + ": " + tiles.get(i).fieldHQ + " : " + tiles.get(i).distances.get(18));
                if(tiles.get(i).myUnits>0){
                    mainTree.run(tiles.get(i));
                }
            }

            System.out.println(Unit.order);
            Unit.order="";
            System.out.println("WAIT");
        }
    }

    public static void updateFields(HashMap<Integer, Tile> tiles, int zoneCount, int myId){
        float DECAY_HQ = 0.8f;//0.8
        float DECAY_MY_UNITS = 0.4f;
        float DECAY_ENEMY_UNITS = 0.9f;
        float DECAY_PLATINUM = 1.9f;//1.8f;//0.3f;//0.9f;
        float DECAY_OWNER = 0.4f;
        float DECAY_UNEXPLORED = 1.8f;



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
            currTile.fighter_score = 2f*currTile.fieldHQ +
                    0.3f*currTile.fieldMyUnits +
                    2f*currTile.fieldEnemyUnits +
                    currTile.fieldPlatinum +
                    currTile.fieldOwner;
            currTile.total_score = currTile.fieldPlatinum +
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
    float fighter_score = 0;
    float explorer_score = 0;

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
    
    public String role=Unit.Explorer;
    int nb_explorer=0;


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
        this.enemyUnits = enemyUnits;
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