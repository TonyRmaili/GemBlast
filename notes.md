
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