//Unit unit=new Unit(0,"fig");
//System.err.println("explorerTree"+Unit.explorerTree.run(unit));
//System.err.println("fighterTree"+Unit.fighterTree.run(unit));


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////           Behavior Tree and units' framework                                 /////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GameState {
    public int myHQ=0;
    public int theirHQ=0;
    public int nbExplorers=0;
    public int nbFighters=0;
    public HashMap<Integer, Tile> tiles;
    public int zoneCount;
}

class Unit {
    public static int max_nb_explorer=10;

    public static final String Fighter = "fig";
    public static final String Explorer = "exp";
    
    public static final String Fighter = "rusher";
    public static final String Explorer = "defenser";
    public static GameState gameState;

    // behavior trees
    public static Node fighterTree;
    public static Node explorerTree;
    // we only need to make them once, this boolean allows to know if the trees are made
    public static boolean treesInitialized = false;

    // tile number in which the unit is
    public int position;
    // whether this unit is an explorer or a fighter
    public String role;
    // the order the unit has to follow
    public String order;

    public Unit(int position,String role){
        this.position=position;
        this.role=role;
        if(role==Unit.Explorer) {
            Unit.gameState.nbExplorers++;
        }
        else if(role==Unit.Explorer) {
            Unit.gameState.nbFighters++;
        }

        if(!Unit.treesInitialized)
        {
            // make trees
            this.initializeTree();
            Unit.treesInitialized=true;
        }
    }

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


    // The run function will return one of the three static stings that tell how the preocess is going
    public abstract String run(Unit unit);
}

// Fallback node: ?
class Fallback extends Node {
    public Node[] childs;

    public Fallback(Node[] childs) {
        this.childs=childs;
    }

    @java.lang.Override
    public String run(Unit unit) {
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
    public String run(Unit unit) {
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
    public String run(Unit unit) {
        //check if the number of explorers is too high (
        if Unit.gameState.nbExplorers>Unit.max_nb_explorer && unit.role!=Unit.Explorer
            return Node.Failure;
        //check if the fighting fild has a higher value
        // TO ADD !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        if false
            return Node.Failure;
        return Node.Success;
    }
}

class ShouldWeRush extends Node {

    @java.lang.Override
    public String run(Unit unit) {
        if Tile.getDistance(Unit.gameState.myHQ,Unit.gameState.theirHQ,tiles)<4
            return Node.Success;
        return Node.Failure;
    }
}


class AmIInHQ extends Node {
    @java.lang.Override
    public String run(Unit unit) {
        if (unit.position==Unit.gameState.myHQ)
            return Node.Success;
        return Node.Failure;
    }
}

class EnemyCloseToHQ extends Node {
    @java.lang.Override
    public String run(Unit unit) {
        if (unit.position==Unit.gameState.myHQ)
            return Node.Success;
        return Node.Failure;
    }
}











//----------------------------------------------------------------------------
// Nodes that are actions













//----------------------------------------------------------------------------
//  Test nodes
class SNode extends Node {
    @java.lang.Override
    public String run(Unit unit) {
        return Node.Success;
    }
}

class FNode extends Node {
    @java.lang.Override
    public String run(Unit unit) {
        return Node.Failure;
    }
}

class RNode extends Node {
    @java.lang.Override
    public String run(Unit unit) {
        return Node.Running;
    }
}