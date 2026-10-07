
GemBlast v2 - Math and Java Evaluation focus

**Presentation**
* NEW FILES
ConfigContent
   game now uses dynamic weight and pay-table adjustment. this file is the GUI element. 

ValueConfig
   Reads from weights.json all configurable values.
   Returns these values from around the app. 
   Idea here is to have an easier way to see and adjust games rtp and other relevant values. Implemented improvements such as 3 stepper buttons and dropped the totalWeight = 100 restriction. 

CsvWriter
   Will change/be replaced. Test file for java Read/Write class

DesktopLauncher
   Launches game via its own main(). This is created because of libGDX and maven not exactly compatible. 
   
MathPlayground
   File for testing methods, mostly from SlothMath independently without needing to compile the whole game.
   Very smooth workflow I am used to from working with Python. 

ShatterPanel
   GUI element for feature shatterCollect. 

SlothMath
   Biggest part of the project. Here is all current and future math methods that run the game. GameEngine and other files uses these methods in the game loop logic.
   

**PROGRESS**
Version 1
* Math methods were all over the place and hard to evaluate/debug. Now they are all contained in SlothMath. 
* RTP base-game: 45% , free-spin: 40% ⇒ total: 85. This needs to bump up to 96% total and ideally more coming from the freeSpin
* Weak feature for super free spin. To increase the RTP here we need more or better features. 
* No proper output for simulator statistics. 
* No good way to configure or balance the values


Version 2
* Goal: main game RTP = 35%, free-spin = 61%  => overall RTP = 96%
* Started by factoring out all the math methods from every file into SlotMath. Wrote them myself. For testing these for bugs I defined MathPlayground were i can run each individual method.
* Once all the methods were implemented the game was runnable and outputting identical values as before. From here I wanted to be able to access the values (mainly reel weights and pay-table) easily and configure them for balancing. ReelWeights.java got replaced by the files ValueConfig and ConfigContent. Game now can make changes to these values, apply and save, all methods relying on these now reads from the weights.json file. Developer can now easily adjust, test, observe. This is not complete, still needs a proper output from simulator to connect the whole workflow for balancing the game. 
* Before putting in work for balancing the game needs more features. At this point I developed two features exclusive for freespin modes (1 for super free only, see **Features** below). After these two features the overall RTP went up to 106.7
  the free spins now contributing around 60% (near the goal) but the base game needs to be brought down. 
* Time to implement the statistics output as CSV and JSON 
* Balance 
* NEW IDEAS for version 3


**Features**
Base-game:
3-3-3-3-3 bet ways
8 paying symbols, 1 wild, 1 scatter

3 scatters on board triggers freeSpin with 10 spins
4+ scatters on board triggers superFreeSpin with 10 spins 

Symbols in a winning combinations gets replaced by new symbols falling in for continued chance of winning (avalanche mechanic Spin stops once there are no winning combos left or no triggered freeSpin. 


FreeSpin mode:
Landing 3+ scatters yields 5 more free-spins for the mode. 
Each winning combo increments the global multiplier by 1 (multiplier starts at 1). Multiple winning combos each increments by 1.
* new feature #1 - shatter Collect * 
All paying symbols now are displayed to the left of the grid with 10 empty fillable meters. Every corresponding symbol in a winning combo (excluding wild) gets collected to the meter. Once filled (up to 10, overflow collects gets carried over for the next meter) the gem "shatters" and pays out MAX_METER x MAX_SYMBOL x GLOBAL_MULTIPLIER. 
  My thoughts here is to have fun thing to happen and build up so the free spin mode doesnt feel dry. This bumps up the RTP in this mode and scales with the multiplier which means at the end of the spin this is were it will pay out the most. 

SuperFreeSpin mode:
Same as free spin with two additional features. 

Sticky Wilds:
Wilds stick around in a spin until they are part of a winning combo, were they get replaced like normal. 

* new feature #2 - uniqueRow 
At the end of a spin (once there are no more cascades going on) the game checks if a row contains 5 unique paying symbols (excluding wilds and scatters). Since there are 3 rows and 8 paying symbols this can occur often. I knew this would pay out a lot and therefore wanted it to be part of the super free rather than freeSpin. 
Payout = MAX_REEL_POSITION * global 
Also scales at the end of the spin mode 

**New Ideas**


1. Void cells (old)
   A rare symbol (similar density to wild) that can be clicked
   opens a choice 1 of 3 hidden cells that contains something useful to complete a winning
   combo
2. Line Combos (old)
   if 5 unique gems land in a row something triggers "something"
3. Diamond grid with locked parts (3-5-7-5-3) => 1575 ways

4. TheoreticalMath were i can output expected values, etc.







