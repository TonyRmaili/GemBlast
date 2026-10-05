
GemBlast v2 - Math and Java Evaluation focus

Current State

1. Math is not in a single big class so it's hard to inspect
2. RTP main-game: 45% , free-spin: 40% ⇒ total: 85


Version 2.0 

1. Break out the game math to a single coherent file that returns the several formats of the calculations (display,monte-carlo, in-game etc.)
2. New GUI element that opens up a weight tuning window for dynamic setting of the numbers
3. Output statistics in CSV or JSON (human-readable format for analysis)
4. Move around 10% units from base game to free-spine mode and increase overall RPT to 96%.
   Goal: main game RTP => 35%, free-spin => 61% (+/- 5%)


New Feature Ideas
1. Void cells
   A rare symbol (similar density to wild) that can be clicked
   opens a choice 1 of 3 hidden cells that contains something useful to complete a winning 
   combo
2. Line Combos
   if 5 unique gems land in a row something triggers "something"



Changes made
1. added CsvWriter for payout and more 
2. Big overhaul on math department. Everything math related should be in the SlotMath.java
3. ValueConfig + UI component. Here we can dynamically adjust the numbers (connect this to sim later for automatic saving of the numbers)

Changes incoming
1. TheoreticalMath were i can output expected values, etc.


SlotMath

1. drawSymbol 
   handles the selection of one symbol per reel.
   output SD? 
2. fillGrid
   fills the grid one symbol at a time. Skips if a cell is flagged as sticky which in current game state only a Wild can sticky in super free mode.

the game works. the numbers however is not were i want them to be, but that was like the original.
overall rtp sitting at 85.45 with +-0.31
bonus entry 1.6%
hit f = 44.85%
avg s per bonus 10,86
multiplier x7.97 / x34

bought super free spin rtp = 46%


** 2 Feature ideas

1. 5 unique gems in a row (with 3 possible rows) provides a "BONUS"
   only triggered in super free spin due to its large probability
   Winning row is not part of a cascasde and evaluates at the end of a cascade. practically being the last thing to happen after a spin
   Win calculation = highest paying symbol in the row treating it as a way 1x1x1x1x1 and last reel. for example if a black diamond is in the row then the win =
   5.0 x 1 x bet = 5 (all the remaining symbols are treated as if they were black diamnods)

   This already raised the overall RTP to 97% 
   Ideas that can improve the rtp in case this was not reaching high engough (could be relevent when i make super mode rarer)
   1. have a multiplier on the posistion of the rarest symbol, for example if black diamond (highest paying) land on reel 5 (highest reel) => max win 
   2. combo per uniqueRow multiplier (getting 1x,2x or 3x the pay once all is resolved of all rows are part of uniqeRow)

1. Something that builds up/collects. Sounds like a fun thing to watch grow and also code.


overall rtp 106.63 with these 2 new features
bought free spin + super pricing balanced towards targeted rtp