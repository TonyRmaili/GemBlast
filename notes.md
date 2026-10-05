
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

My idea moving forward (we wont do them now)

move around 10% rtp from base game to free spin
increase super spin rtp so the overall rtp lands on 96
invent 2 more features (here one of them can be super spin mode only)
Pipeline a much more advanced statistics file as csv and json (identical data, differant format) as requested by my boss
Change the dynamic config part so it is better to use (right now clunky and to much clutter like the images). I am also thinking that the weight total needs not be 100 since we are dividing by the total. 100 is just a nicer number to understand., This makes the guard in config not needed.
Build the MathPlayground a bit better to prepare for my demonstration (like how i can run things separetly and so on)
Bug test, validate and implement the changes mentioned above make sure they work
One more thing about the math part. Seeing how much statistics the simulator gives out, its only a fraction of the math i just did in SlotMath. Would like to move those calculations out there to so i can do them myself (why did you not place them there in the first place?