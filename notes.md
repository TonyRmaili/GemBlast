
GemBlast v2 - Math and Java Evaluation focus

* NEW FILES
ConfigContent
   game now uses dynamic weight and pay-table adjustment. this file is the GUI element. 

ValueConfig
   Reads from weights.json all configurable values.
   Returns these values from around the app. 
   Idea here is to have an easier way to see and adjust games rtp and other relevant values. Implemented improvements such as 3 stepper buttons and dropped the totalWeight = 100 restriction. 

GemWriter + DataWriter + ChartWriter
   Data and Chart outputs the results (json, csv and text from Data and graphs from Chart) while GemWriter orchestrates all the writing methods and handles naming and paths


DesktopLauncher
   Launches game via its own main(). This is created because of libGDX and maven not exactly compatible. 
   
MathPlayground
   File for testing methods, from SlothMath + TheoreticalMath ; independently without needing to compile the whole game.
   Workflow I am used to from working with Python. 

ShatterPanel
   GUI element for the new feature shatterCollect. 

SlothMath
   Biggest part of the project. Here is all current and future math methods that run the game. GameEngine and other files uses these methods in the game loop logic.
   
TheoryMath
   for fun and exercise here the idea is to replicate some values such as rtp, expected value, variance etc. by doing calculations from set weights and probability without needing to use the simulation. Good way to check against the simulation values too. 

simulator side ──uses──▶ GameEngine + SlotMath ◀──uses── normal game

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

* Before putting in work for balancing the game needs more features. At this point I developed two features exclusive for freespin modes (1 for super free only, see **Features** below). After these two features the overall RTP went up to 106.7 from 85.x% 
  the free spins now contributing around 60% (near the goal) but the base game needs to be brought down. 

* To make the balancing easier i wanted to try having different forms of outputs that help read the values used once i change the configs. New file GemWriter replaced CsvWriter and has an all-in-one output with text, csv and json of the same result.

* Balancing summary:
  <96% reached. Reduced the scatter was a big deal. Reduced the density of low paying symbols (they were high) and reduced paytable to zone in on 96>
  for details see output folder
   
   This approach was straight forward. It worked but I can see how this is not acceptable. If reducing hit frequencies and lowering paytables comes at the cost of making the game boring/dull/ no exciting then there are many other ways to balance. 
   Every constant defined in SlothMath fields can technically be tuned e.g. RETRIGGER_SCATTERS,MULTIPLIER_START etc. or some more detailed approaches like increase/decrease payout from uniqueRow/shatterCollect (any feature really)
   For a more complex game were cells can be locked/unlocked and more things trigger (example look at Feature POLIS from SS) perhaps then we need to look at other than these straight forward values to tune 

   
  
**Features**
Base-game:
3-3-3-3-3 bet ways
8 paying symbols, 1 wild, 1 scatter

3 scatters on board triggers freeSpin with 10 spins
4+ scatters on board triggers superFreeSpin with 10 spins 

Symbols in a winning combinations gets replaced by new symbols falling in for continued chance of winning (avalanche mechanic Spin stops once there are no winning combos left or no triggered freeSpin. 


FreeSpin mode:
Landing 3+ scatters yields 5 more free-spins for the mode. 
Each cascade increments the global multiplier by 1 (multiplier starts at 1). Multiple winning combos each increments by 1.
* new feature #1 - shatter Collect * 
All paying symbols now are displayed to the left of the grid with 10 empty fillable meters. Every corresponding symbol in a winning combo (excluding wild) gets collected to the meter. Once filled (up to 10, overflow collects gets carried over for the next meter) the gem "shatters" and pays out MAX_METER x MAX_SYMBOL x GLOBAL_MULTIPLIER
  My thoughts here is to have fun thing to happen and build up so the free spin mode doesn't feel dry. This bumps up the RTP in this mode and scales with the multiplier which means at the end of the spin this is where it will pay out the most. 

SuperFreeSpin mode:
Same as free spin with two additional features. 

Sticky Wilds:
Wilds stick around in a spin until they are part of a winning combo, were they get replaced like normal. 

* new feature #2 - uniqueRow 
At the end of a spin (once there are no more cascades going on) the game checks if a row contains 5 unique paying symbols (excluding wilds and scatters). Since there are 3 rows and 8 paying symbols this can occur often. I knew this would pay out a lot and therefore wanted it to be part of the super free rather than freeSpin. 
Payout = MAX_REEL_POSITION * global 
Also scales at the end of the spin mode 

  


**Version3** 

1. charts
2. theoryMath

**Charts** 

* rtp_convergence - 

* rtp_contribution - how much of the 96% each win size pays (the bars add up to exactly the RTP)

* win_exceedance - the chance a round wins at least X

* win_histogram - (log-log): the share of rounds per win size.

     



**Future Ideas**

** Diamond Grid ** 

   current shape: 3^5 = 243 ways

   [] [] [] [] []
   [] [] [] [] []
   [] [] [] [] []


   f = free spin
   s = super + free spin
   otherwise cells are locked
   
   if base-game is in need of more rtp; 1 cell unlock per scatter? were 3 scatter for a full unlock through freespin mode

   Cut diamond shape: 3*4*5*4*3 = 720
   
   [x] [x] [x] [x] [x]
   [x] [x] [x] [x] [x]
   [x] [x] [x] [x] [x]
       [f] [f] [f]
           [s]
   
   
      Royal cut shape: 1575 ways
           [f]
       [f] [f] [f]
   [x] [x] [x] [x] [x]
   [x] [x] [x] [x] [x]
   [x] [x] [x] [x] [x]
       [s] [s] [s]
           [s]



** Interactive Cells / Reels ** 

1. A new symbol (a joker). Very rare and base-game exclusive
Limited to once per grid? 
Once it lands game pauses; 3+ chooses are presented; behind them is a multiple of some win (always wins)
This could be made entirely different ; see reel idea below

2. Game on reel. Seen this once in another game (not nolimit; but could be there too)
Every reel can be re-rolled and COSTS the player X bet weighted by fair-price of odds of every win it can help complete


**AI balancing agent**

An idea for automating balancing the game. Purpose to save time and "prune" branches of values not worth investigating.
A similar idea to d/d(xn) (f(x1,x2,..,xn)) = 0 for multi variable case (find local minimum)
A rapport with tried weights and their results is sent to AI; it decided which ones to try further > access valueConfig file with new values as input > loops over X amount for safety

Great way to "let the game tune itself" over the weekend. 
At the end there can be a full rapport for developer of the progress
Highlight acceptable values or "path trends"
Throw in some graphs for visual aid per rapport. 

** New Game ideas ** 
--------------------------
Pay mechanic : Collect pays
Theme: "Conan the barbarian style / sword and sorcery / "
Character: Big muscular Barbarian male. "Hot" sorceress female, "Gritty" dwarf male. Some "Fantasy creature" evil looking (kobold/dragonling) Goblinoid

keywords: "gritty", "old school" pixel art with modern take. Greed / fighting for loot and treasure

Gameplay: player chooses their main avatar (sets some multiplier higher for that character)
   mechanically all characters function the same with only different animations in "combat"/"collect"
   favoured/selected character yields best result (illusion of choice, superstition and excitement)

Features: Artifacts/magical items exist in the game, a separate "build-up" during a spin other than just collecting treauser(wins)
   They ultimately serve as multipliers but triggers different events. 

--------------------------








