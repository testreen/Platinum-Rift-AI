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
            GameState.turn=turn;
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
        float DECAY_PLATINUM = 1.6f;//1.8f;//0.3f;//0.9f;
        float DECAY_OWNER = 0.4f;
        float DECAY_UNEXPLORED = 1.2f;



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

    String role=Unit.Explorer;


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





//Unit unit=new Unit(0,"fig");
//System.err.println("explorerTree"+Unit.explorerTree.run(unit));
//System.err.println("fighterTree"+Unit.fighterTree.run(unit));


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////           Behavior Tree and units' framework                                 /////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


class GameState {
    public static int myId=0;
    public static int myHQ=0;
    public static int enemyHQ=0;
    public static int nbExplorers=0;
    public static int nbFighters=0;
    public static HashMap<Integer, Tile> tiles;
    public static int zoneCount;
    public static int rushArmySize=6;
    public static int theirRushArmySize=0;
    public static int turn = 0;
}

class Unit {
    public static int max_nb_explorer=20;

    public static final String Fighter = "fig";
    public static final String Explorer = "exp";

    public static final String Rusher = "rusher";
    public static final String Defender = "defender";

    public static final String NoRole = "none";

    // behavior trees
    public static Node fighterTree;
    public static Node explorerTree;
    // we only need to make them once, this boolean allows to know if the trees are made
    public static boolean treesInitialized = false;

    public static String order="";

    private void initializeTree() {
        ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO
        Unit.explorerTree=new Fallback(new Node[]{new FNode(),new FNode(),new RNode()});
        //Unit.explorerTree=new Fallback(new Node[]{new FNode(),new RNode(),new SNode()});
        Unit.fighterTree=new Sequence(new Node[]{new SNode(),new SNode(),new SNode()});
    }

}


// This abstract class represents a Node of the behavior tree no matter which kind, all nodes will inherit this
abstract class Node {
    public static final String Success="success";
    public static final String Running="running";
    public static final String Failure="failure";

    public static int distance_rush=6;


    // The run function will return one of the three static stings that tell how the preocess is going
    public abstract String run(Tile unit);
}

// Fallback node: ?
class Fallback extends Node {
    public Node[] childs;

    public Fallback(Node[] childs) {
        this.childs=childs;
    }

    @java.lang.Override
    public String run(Tile unit) {
        String res = "";
        for(Node child:this.childs){
            res=child.run(unit);
            if(res==Node.Success){
                return Node.Success;
            }
            else if(res==Node.Running){
                return Node.Running;
            }
        }
        return Node.Failure;
    }
}


// Sequence node: ->
class Sequence extends Node {
    public Node[] childs;

    public Sequence(Node[] childs) {
        this.childs=childs;
    }

    @java.lang.Override
    public String run(Tile unit) {
        String res = "";
        for(Node child:this.childs){
            res=child.run(unit);
            if(res==Node.Failure){
                return Node.Failure;
            }
            else if(res==Node.Running){
                return Node.Running;
            }
        }
        return Node.Success;
    }
}


//----------------------------------------------------------------------------
// Nodes that are conditions

class ShouldIExplore extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        //check if the number of explorers is too high (
        if (GameState.nbExplorers>Unit.max_nb_explorer && unit.role!=Unit.Explorer)
            return Node.Failure;
        //check if the fighting fild has a higher value
        // TO ADD !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (false)
            return Node.Failure;
        return Node.Success;
    }
}

class ShouldWeRush extends Node {

    @java.lang.Override
    public String run(Tile unit) {
        if(GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ)>Node.distance_rush)
            return Node.Failure;
        if(GameState.rushArmySize<=1 && GameState.theirRushArmySize>1){
            System.err.println("Stopping rush");
            return Node.Failure;
        }

        if(GameState.turn>=GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ)+6){
            System.err.println("Stopping rush");
            return Node.Failure;
        }
        return Node.Success;
    }
}

class PostRush extends Node {

    @java.lang.Override
    public String run(Tile unit) {

        if(GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ)<=Node.distance_rush && GameState.rushArmySize<=1){
            System.err.println("Post rush");
            return Node.Success;
        }
        return Node.Failure;
    }
}


class AmIInHQ extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        if (unit.id==GameState.myHQ)
            return Node.Success;
        return Node.Failure;
    }
}

class EnemyCloseToHQ extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        return Node.Failure;
    }
}











//----------------------------------------------------------------------------
// Nodes that are actions


class Explore extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        System.err.println("Exploring tile"+Integer.toString(unit.id));
        int i =unit.id;
        List<Integer> list = GameState.tiles.get(i).linkedTiles; // neighbours in list
        List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
        Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
        Random rand = new Random();

        // initialize lists and best score
        float bestScore = GameState.tiles.get(i).total_score;
        float bestHQfield = GameState.tiles.get(i).fieldHQ;
        int bestHQ = i;
        int bestId = i;
        int units = GameState.tiles.get(i).myUnits;
        next.add(i);
        // if > 10 units, save 2nd best tile too
        if(GameState.tiles.get(i).myUnits >=3){
            next.add(i);
        }

        for(int l=0; l < list.size(); l++){

            // if neighbour is not under our control
            if (GameState.tiles.get(list.get(l)).ownerId != GameState.myId) {
                // if enemy unit is close, leave up to 4 units to avoid getting passed
                if(GameState.tiles.get(list.get(l)).enemyUnits > 1 && (!GameState.tiles.get(list.get(l)).enemyHQ || GameState.tiles.get(i).myHQ)){
                    for(int p=0; p < Math.min(4, GameState.tiles.get(list.get(l)).enemyUnits); p++){
                        next.add(i);
                    }
                } else if(GameState.tiles.get(list.get(l)).enemyUnits == 1){
                    //next.add(i);
                }
                // add not controlled area to list of moves



                int count=0;
                for(int n : GameState.tiles.get(list.get(l)).linkedTiles)
                {
                    if(n != i && (GameState.tiles.get(n).platinumSource > 0 || GameState.tiles.get(n).visible != 1))
                    {
                        count++;
                        //System.err.println("Cell "+Integer.toString(i)+" cell counted: "+Integer.toString(n));
                    }

                }
                //System.err.println("Cell "+Integer.toString(i)+" neighbor:"+Integer.toString(list.get(l))+" count: "+Integer.toString(count));
                if(count != 0 || GameState.tiles.get(list.get(l)).platinumSource > 0) {
                    next.add(arr[l]);
                }
                //next.add(arr[l]);
            }


            // if new best tile to move to
            if (GameState.tiles.get(list.get(l)).total_score > bestScore) {
                // if over 10 units, also save a 2nd best
                if(GameState.tiles.get(i).myUnits >=3){
                    next.set(1, next.get(0));
                }
                bestScore = GameState.tiles.get(list.get(l)).total_score;
                bestId = list.get(l);
                next.set(0, arr[l]);
            }

            if (GameState.tiles.get(list.get(l)).fieldHQ > bestHQfield){
                bestHQfield = GameState.tiles.get(list.get(l)).fieldHQ;
                bestHQ = list.get(l);
            }


        }
        if(bestScore < 0){
            next.set(0, bestHQ);
        }

        // assign unit moves
        for(int k = 0; k < GameState.tiles.get(i).myUnits; k++){
            int j;
            // if only one unit in tile and has neighbors not under our control,
            // move to one of them randomly
            if(GameState.tiles.get(i).myUnits == 1 && next.size() > 1){
                int bestPlat = 0;
                int bestPlatIndex = rand.nextInt(next.size() - 1) + 1;
                for(int l = 1; l < next.size(); l++){
                    if(GameState.tiles.get(next.get(l)).platinumSource > bestPlat && GameState.tiles.get(next.get(l)).ownerId != GameState.myId){
                        bestPlatIndex = l;
                        bestPlat = GameState.tiles.get(next.get(l)).platinumSource;
                    }
                }
                j = next.get(bestPlatIndex);
            }

            // if no more forced moves, move to best tile
            else if(k > next.size() - 1){
                j = next.get(0);

                // if forced move
            } else if(GameState.tiles.get(i).myUnits > 1 && next.size() > 1){
                j = next.get(k);
            } else {
                j = next.get(0);
            }
            // to avoid error if staying in same tile
            if(i != j){
                Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
            }
        }

        return Node.Success;

    }
}

class ExplorePostRush extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        System.err.println("Exploring tile"+Integer.toString(unit.id));
        int i =unit.id;
        List<Integer> list = GameState.tiles.get(i).linkedTiles; // neighbours in list
        List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
        Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
        Random rand = new Random();

        // initialize lists and best score
        float bestScore = 2*GameState.tiles.get(i).fieldPlatinum+GameState.tiles.get(i).fieldOwner;
        float bestHQfield = GameState.tiles.get(i).fieldHQ;
        int bestHQ = i;
        int bestId = i;
        int units = GameState.tiles.get(i).myUnits;
        next.add(i);
        // if > 10 units, save 2nd best tile too
        if(GameState.tiles.get(i).myUnits > 10){
            next.add(i);
        }

        for(int l=0; l < list.size(); l++){
            // if neighbour is not under our control
            if (GameState.tiles.get(list.get(l)).ownerId != GameState.myId) {
                // if enemy unit is close, leave up to 4 units to avoid getting passed
                if(GameState.tiles.get(list.get(l)).enemyUnits > 1 && (!GameState.tiles.get(list.get(l)).enemyHQ || GameState.tiles.get(i).myHQ)){
                    for(int p=0; p < Math.min(4, GameState.tiles.get(list.get(l)).enemyUnits); p++){
                        next.add(i);
                    }
                } else if(GameState.tiles.get(list.get(l)).enemyUnits == 1){
                    //next.add(i);
                }
                // add not controlled area to list of moves
                next.add(arr[l]);
            }

            // if new best tile to move to
            if (2*GameState.tiles.get(list.get(l)).fieldPlatinum+GameState.tiles.get(list.get(l)).fieldOwner > bestScore) {
                // if over 10 units, also save a 2nd best
                if(GameState.tiles.get(i).myUnits > 10){
                    next.set(1, next.get(0));
                }
                bestScore = 2*GameState.tiles.get(list.get(l)).fieldPlatinum+GameState.tiles.get(list.get(l)).fieldOwner;
                bestId = list.get(l);
                next.set(0, arr[l]);
            }

            if (GameState.tiles.get(list.get(l)).fieldHQ > bestHQfield){
                bestHQfield = GameState.tiles.get(list.get(l)).fieldHQ;
                bestHQ = list.get(l);
            }


        }
        if(bestScore < 0){
            next.set(0, bestHQ);
        }

        // assign unit moves
        for(int k = 0; k < GameState.tiles.get(i).myUnits; k++){
            int j;
            // if only one unit in tile and has neighbors not under our control,
            // move to one of them randomly
            if(GameState.tiles.get(i).myUnits == 1 && next.size() > 1){
                int bestPlat = 0;
                int bestPlatIndex = rand.nextInt(next.size() - 1) + 1;
                for(int l = 1; l < next.size(); l++){
                    if(GameState.tiles.get(next.get(l)).platinumSource > bestPlat && GameState.tiles.get(next.get(l)).ownerId != GameState.myId){
                        bestPlatIndex = l;
                        bestPlat = GameState.tiles.get(next.get(l)).platinumSource;
                    }
                }
                j = next.get(bestPlatIndex);
            }

            // if no more forced moves, move to best tile
            else if(k > next.size() - 1){
                j = next.get(0);

                // if forced move
            } else if(GameState.tiles.get(i).myUnits > 1 && next.size() > 1){
                j = next.get(k);
            } else {
                j = next.get(0);
            }
            // to avoid error if staying in same tile
            if(i != j){
                Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
            }
        }

        return Node.Success;

    }
}


class Rush extends Node {
    int dist;
    int turn=0;
    int armyLoc=-1;
    public int[] explorer = new int[4];
    @java.lang.Override
    public String run(Tile unit) {
        this.dist=GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ);
        this.turn++;
        if(turn==1)
            this.armyLoc=GameState.myHQ;
        List<Integer> list;
        int min;
        int best=0;
        if(this.armyLoc!=GameState.enemyHQ)
        {
            min=dist;
            best=0;
            list = GameState.tiles.get(armyLoc).linkedTiles;
            for(int l=0; l < list.size(); l++){
                if(GameState.tiles.get(list.get(l)).distances.get(GameState.enemyHQ)<=min) {
                    min=GameState.tiles.get(list.get(l)).distances.get(GameState.enemyHQ);
                    best=list.get(l);
                }
            }

            GameState.rushArmySize=Math.min(6,GameState.tiles.get(armyLoc).myUnits);
            GameState.theirRushArmySize=GameState.tiles.get(armyLoc).enemyUnits;
            Unit.order += Integer.toString(Math.min(6,GameState.tiles.get(armyLoc).myUnits))+" " + Integer.toString(armyLoc) + " " + Integer.toString(best) + " ";
            this.armyLoc=best;
        }


        if(dist>1)
        {
            System.err.println("distance>1");
            ////////////////////////////////////////////////////////////////
            if(this.turn==1)
            {
                System.err.println("if1");
                int i =unit.id;
                list = GameState.tiles.get(i).linkedTiles; // neighbours in list
                List<Integer> visited = new ArrayList<Integer>();

                int count=0;
                for(int l=0; l < list.size(); l++){
                    if(!visited.contains(list.get(l)) && list.get(l)!=this.armyLoc && count<4) {
                        visited.add(list.get(l));
                        Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(list.get(l)) + " ";
                        this.explorer[count]=list.get(l);
                        count++;
                    }
                    if(count>=4)
                        break;
                }
                while(count<4){

                    for(int l=0; l < list.size(); l++){
                        visited.add(list.get(l));
                        Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(list.get(l)) + " ";
                        this.explorer[count]=list.get(l);
                        count++;
                        if(count>=4)
                            break;
                    }


                }
            }
            else if(turn<=this.dist/2 && GameState.theirRushArmySize<GameState.rushArmySize)
            {
                System.err.println("if2");
                for(int m = 0; m < 4; m++){
                    int i =this.explorer[m];
                    list = GameState.tiles.get(i).linkedTiles; // neighbours in list
                    List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
                    Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
                    Random rand = new Random();

                    // initialize lists and best score
                    float bestScore = GameState.tiles.get(i).fieldPlatinum+GameState.tiles.get(i).fieldUnexplored;
                    float bestHQfield = GameState.tiles.get(i).fieldHQ;
                    int bestHQ = i;
                    int bestId = i;
                    int units = GameState.tiles.get(i).myUnits;

                    for(int l=0; l < list.size(); l++){


                        // if new best tile to move to
                        if (GameState.tiles.get(list.get(l)).fieldPlatinum+GameState.tiles.get(list.get(l)).fieldUnexplored > bestScore) {
                            // if over 10 units, also save a 2nd best
                            if(GameState.tiles.get(i).myUnits > 1 && next.size()>1){
                                next.set(1, next.get(0));
                            }
                            bestScore = GameState.tiles.get(list.get(l)).fieldPlatinum+GameState.tiles.get(list.get(l)).fieldUnexplored;
                            bestId = list.get(l);
                            if(next.size()>0)
                                next.set(0, arr[l]);
                            else
                                next.add(arr[l]);
                        }

                        if (GameState.tiles.get(list.get(l)).fieldHQ > bestHQfield){
                            bestHQfield = GameState.tiles.get(list.get(l)).fieldHQ;
                            bestHQ = list.get(l);
                        }


                    }

                    // assign unit moves

                    int j=-1;
                    // if only one unit in tile and has neighbors not under our control,
                    // move to one of them randomly
                    for(int k=0;k<next.size();k++)
                    {
                        boolean test=false;
                        for(int nm=0;nm<m;nm++)
                        {
                            if(next.get(k)==this.explorer[nm])
                                test=true;
                        }
                        if(!test)
                        {
                            j=next.get(k);
                            break;
                        }
                    }
                    if(j==-1)
                        j=next.get(0);
                    // to avoid error if staying in same tile
                    if(i != j){
                        Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(j) + " ";
                        this.explorer[m]=j;
                    }

                }
            }
            else if(turn<=dist){
                System.err.println("if3");
                for(int m = 0; m < 4; m++){
                    int i =this.explorer[m];
                    if(i==GameState.myHQ) {
                        System.err.println("BEAK");
                        continue;
                    }
                    System.err.println("explo:"+Integer.toString(i));
                    min=dist;
                    best=0;
                    list = GameState.tiles.get(i).linkedTiles;
                    for(int l=0; l < list.size(); l++){
                        System.err.println("explorer "+Integer.toString(i)+"  focus "+Integer.toString(list.get(l)));

                        if(GameState.tiles.get(list.get(l)).distances.get(GameState.myHQ)<=min) {
                            min=GameState.tiles.get(list.get(l)).distances.get(GameState.myHQ);
                            best=list.get(l);
                        }
                        if(list.get(l)==GameState.myHQ) {
                            System.err.println("HQ found");
                            best=GameState.myHQ;
                            min=-1;
                        }
                    }
                    Unit.order += Integer.toString(Math.min(1,GameState.tiles.get(i).myUnits))+" " + Integer.toString(i) + " " + Integer.toString(best) + " ";
                    this.explorer[m]=best;
                }
            }
            //////////////////////////////////////////////////////////////
        }



        return Node.Success;
    }
}









//----------------------------------------------------------------------------
//  Test nodes
class SNode extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        return Node.Success;
    }
}

class FNode extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        return Node.Failure;
    }
}

class RNode extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        return Node.Running;
    }
}