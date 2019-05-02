# Platinum-Rift-AI
AI for playing Platinum Rift 2 (https://www.codingame.com/ide/puzzle/platinum-rift-episode-2)

Multi-Agent Potential Fields:
1) Identifying the objects
    - Our own occupied tiles (Updated every turn)
    - Unoccupied tiles (Updated every turn)
    - Tiles occupied by enemy (Can only be updated if visible)
    - Our PODs (Updated every turn)
    - Enemy PODs (Can only be updated if visible)
    - Platinum (Static but can only be updated if visible)
    - Our Headquarter (Can't identify after first round so needs to be saved)
    - Enemy Headquarter (Can't identify after first round so needs to be saved)

2) Identifying the fields
    - Defend our own headquarter
    - Take enemy headquarter
    - Occupy Platinum filled tiles
    - Explore cells
    (Field of navigation, Strategic field, Tactical field and Field of exploration)

3) Assigning the charges
    Positive charges:
        - Unoccupied tiles
        - Tiles occupied by enemy
        - Our PODs
        - Enemy PODs
        - Platinum
        - Enemy Headquarter
    Negative charges:
        - Our own occupied tiles
        - Our Headquarter

4) Deciding on the granularities
    - Not applicable to a discrete graph like this case

5) Agentifying the core objects
    - One agent in each POD
    - Simulate enemy PODs movement(?)

6) Construct the MAS Architecture
    - Interface agent
    - Attack coordinating agent

7) Fog of war considerations
    - Remember enemy positions
    - Expand enemy positions through fog of war
    - Exploration
