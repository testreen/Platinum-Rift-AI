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
}

class Unit {
    public static int max_nb_explorer=10;

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
        {
            unit.role=Unit.Fighter;
            return Node.Failure;
        }
        
        //check if the fighting fild has a higher value
        // TO ADD !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if (false)
            return Node.Failure;
        if (unit.role!=Unit.Explorer)
        {
            GameState.nbExplorers+=unit.myUnits;
            unit.role=Unit.Explorer;
        }
        
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
                for(int n:GameState.tiles.get(list.get(l)).linkedTiles)
                {
                    if(n!=i && (GameState.tiles.get(n).platinumSource>0 || GameState.tiles.get(n).visible!=1))
                    {
                        count++;
                        System.err.println("Cell "+Integer.toString(i)+" cell counted: "+Integer.toString(n));
                    }
                
                }
                System.err.println("Cell "+Integer.toString(i)+" neighbor:"+Integer.toString(list.get(l))+" count: "+Integer.toString(count));
                if(count!=0)
                    next.add(arr[l]);
                    
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
                j = next.get(rand.nextInt(next.size() - 1) + 1);
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


class ExploreSimple extends Node {
    @java.lang.Override
    public String run(Tile unit) {
        System.err.println("Exploring tile"+Integer.toString(unit.id));
        int i =unit.id;
        List<Integer> list = GameState.tiles.get(i).linkedTiles; // neighbours in list
        List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
        Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
        Random rand = new Random();
        HashMap<Float, Integer> closeTiles = new HashMap<Float, Integer>();
        

        // initialize lists and best score
        float bestScore = GameState.tiles.get(i).explorer_score;
        float bestHQfield = GameState.tiles.get(i).fieldHQ;
        int bestHQ = i;
        int bestId = i;
        int units = GameState.tiles.get(i).myUnits;
        closeTiles.put(GameState.tiles.get(i).explorer_score,i);

        for(int l=0; l < list.size(); l++){
            // if new best tile to move to
            closeTiles.put(GameState.tiles.get(list.get(l)).explorer_score,list.get(l));

        }
                           
        
                           
        ArrayList<Float> sortedKeys = new ArrayList<Float>(closeTiles.keySet()); 
          
        Collections.sort(sortedKeys); 
        Collections.reverse(sortedKeys);              
       
        // assign unit moves
        for(int k = 0; k < GameState.tiles.get(i).myUnits; k++){
            if(k<sortedKeys.size())
            {
                Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(closeTiles.get(sortedKeys.get(k))) + " ";
                GameState.tiles.get(closeTiles.get(sortedKeys.get(k))).role=Unit.Explorer;
            }
            else {
                Unit.order += "1 " + Integer.toString(i) + " " + Integer.toString(closeTiles.get(sortedKeys.get(0))) + " ";
                GameState.tiles.get(closeTiles.get(sortedKeys.get(0))).role=Unit.Explorer;
            }
            
        }
        
        return Node.Success;
        
    }
}

class Fight extends Node{
    @java.lang.Override
    public String run(Tile unit) {
        System.err.println("Fighting tile"+Integer.toString(unit.id));
        int i =unit.id;
        List<Integer> list = GameState.tiles.get(i).linkedTiles; // neighbours in list
        List<Integer> next = new ArrayList<Integer>(); // list of moves from tile
        Integer[] arr = list.toArray(new Integer[list.size()]); // neighbours in array
        Random rand = new Random();
        HashMap<Float, Integer> closeTiles = new HashMap<Float, Integer>();
        

        // initialize lists and best score
        float bestScore = GameState.tiles.get(i).fighter_score;
        float bestHQfield = GameState.tiles.get(i).fieldHQ;
        int bestHQ = i;
        int bestId = i;
        int units = GameState.tiles.get(i).myUnits;
        closeTiles.put(GameState.tiles.get(i).fighter_score,i);

        for(int l=0; l < list.size(); l++){
            // if new best tile to move to
            closeTiles.put(GameState.tiles.get(list.get(l)).fighter_score,list.get(l));

        }
                           
        
                           
        ArrayList<Float> sortedKeys = new ArrayList<Float>(closeTiles.keySet()); 
          
        Collections.sort(sortedKeys); 
        Collections.reverse(sortedKeys);              
       
        // assign unit moves
        Unit.order += Integer.toString(unit.myUnit)+" " + Integer.toString(i) + " " + Integer.toString(closeTiles.get(sortedKeys.get(0))) + " ";
           GameState.tiles.get(closeTiles.get(sortedKeys.get(0))).role=Unit.Fighter;
          
        
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
                j = next.get(rand.nextInt(next.size() - 1) + 1);
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


class ExploreOld extends Node {
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
            if (GameState.tiles.get(list.get(l)).total_score > bestScore) {
                // if over 10 units, also save a 2nd best
                if(GameState.tiles.get(i).myUnits > 10){
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
        if(GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ) < 5 || (GameState.zoneCount < 50 && GameState.tiles.get(GameState.myHQ).distances.get(GameState.enemyHQ) < 7)){
            next = new ArrayList<Integer>(); // list of moves from tile
            if(i == GameState.myHQ){


                if(GameState.tiles.get(i).myUnits >= 4){
                    for(int p=0; p < 4; p++){
                        next.add(i);
                    }
                    for(int p=0; p < GameState.tiles.get(i).myUnits - 4; p++){
                        next.add(bestHQ);
                    }
                } else if(GameState.tiles.get(i).myUnits > 1) {
                    for(int p=0; p < GameState.tiles.get(i).myUnits - 1; p++){
                        next.add(bestHQ);
                    }
                    next.add(i);
                } else {
                    next.add(i);
                }
            } else {
                for(int p=0; p < GameState.tiles.get(i).myUnits; p++){
                    next.add(bestHQ);
                }
            }
        }

        // assign unit moves
        for(int k = 0; k < GameState.tiles.get(i).myUnits; k++){
            int j;
            // if only one unit in tile and has neighbors not under our control,
            // move to one of them randomly
            if(GameState.tiles.get(i).myUnits == 1 && next.size() > 1){
                j = next.get(rand.nextInt(next.size() - 1) + 1);
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