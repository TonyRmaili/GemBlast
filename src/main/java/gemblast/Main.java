package gemblast;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.Random;

/**
 * Gem Blast: entry point. Builds the screen and connects the buttons to the game model.
 * "implements ReelScreen.SpinListener" = Main promises to have onWinShown() and onMultiplierChanged()
 * (and chooses to also write the optional onShatterMeters() / onShatterExplode()),
 * so it can pass itself (this) to ReelScreen and get told when wins appear.
 */
public class Main extends ApplicationAdapter implements ReelScreen.SpinListener {

    // ---------------------------------------------------------------- constants
    // Virtual screen size. FitViewport scales it to any window size.
    private static final float WORLD_WIDTH       = 1280f;
    private static final float WORLD_HEIGHT      = 720f;
    private static final float BUTTON_BAR_HEIGHT = 120f;   // space kept free at the bottom for buttons
    private static final float TOP_BAR_HEIGHT    = 80f;    // space kept free at the top for WIN / free spins displays
    private static final float MARGIN            = 12f;

    private static final int REELS = 5;
    private static final int ROWS  = 3;

    private static final long BET = 1_00;   // 1.00 unit, in cents (see Wallet for why cents)

    // ---------------------------------------------------------------- game model (no graphics)
    private final Random rng = new Random();
    private final Wallet wallet = new Wallet();
    private final ValueConfig config = ValueConfig.load();         // weights.json in the project root
    private final SlotMath math = new SlotMath(config);            // ALL the maths
    private final GameEngine engine = new GameEngine(math, REELS, ROWS);
    private boolean spinning = false;
    private GameMode activeBooster = GameMode.NORMAL;   // NORMAL = no booster on

    // ---------------------------------------------------------------- screen (libGDX)
    private Stage stage;
    private ReelScreen reelScreen;
    private Button spinButton;
    private Button resetButton;
    private Button menuButton;
    private Button shopButton;
    private Button simButton;
    private Button configButton;
    private AmountDisplay balanceDisplay;
    private AmountDisplay winDisplay;
    private AmountDisplay freeSpinsDisplay;   // "3 / 10", only visible during free spins
    private AmountDisplay multiplierDisplay;  // "x4",     only visible during free spins
    private AmountDisplay boosterDisplay;     // shows the active booster (same spot as FREE SPINS)
    private ShatterPanel shatterPanel;        // SHATTER meters, left of the reels, only visible during free spins
    private InfoPanel infoPanel;
    private InfoPanel shopPanel;              // rebuilt every time the shop opens
    private InfoPanel simPanel;               // built once, so the last results stay visible
    private InfoPanel configPanel;
    private long shownWin;                  // what the WIN display is currently counting up to
    private int freeSpinsTotal;             // spins awarded so far in the current bonus (grows on retrigger)


    @Override
    public void create() {
        Assets.load();

        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        Gdx.input.setInputProcessor(stage);   // without this, clicks never reach the buttons

        // Reel area: everything between the top bar and the button bar, with a small margin.
        float areaWidth  = WORLD_WIDTH - MARGIN * 2f;
        float areaHeight = WORLD_HEIGHT - BUTTON_BAR_HEIGHT - TOP_BAR_HEIGHT - MARGIN * 2f;
        reelScreen = new ReelScreen(REELS, ROWS, areaWidth, areaHeight, 12f);
        reelScreen.setPosition(
                (WORLD_WIDTH - reelScreen.getWidth()) / 2f,
                BUTTON_BAR_HEIGHT + (WORLD_HEIGHT - BUTTON_BAR_HEIGHT - TOP_BAR_HEIGHT - reelScreen.getHeight()) / 2f);
        stage.addActor(reelScreen);

        // Left of the reels: the SHATTER meters, hidden outside free spins.
        shatterPanel = new ShatterPanel(config);
        shatterPanel.setPosition(reelScreen.getX() / 2f, reelScreen.getY(Align.center), Align.center);
        shatterPanel.setVisible(false);
        stage.addActor(shatterPanel);

        // Top bar: WIN display, hidden until the round wins something.
        winDisplay = new AmountDisplay("WIN", 260f, 70f);
        winDisplay.setPosition(WORLD_WIDTH / 2f, WORLD_HEIGHT - TOP_BAR_HEIGHT / 2f, Align.center);
        winDisplay.setVisible(false);
        stage.addActor(winDisplay);

        // Either side of WIN: free spins counter and multiplier, hidden outside the bonus.
        freeSpinsDisplay = new AmountDisplay("FREE SPINS", 200f, 70f);
        freeSpinsDisplay.setPosition(winDisplay.getX() - 20f, winDisplay.getY(Align.center), Align.right);
        freeSpinsDisplay.setVisible(false);
        stage.addActor(freeSpinsDisplay);

        multiplierDisplay = new AmountDisplay("MULTIPLIER", 200f, 70f);
        multiplierDisplay.setPosition(winDisplay.getRight() + 20f, winDisplay.getY(Align.center), Align.left);
        multiplierDisplay.setVisible(false);
        stage.addActor(multiplierDisplay);

        // Booster indicator: shares the FREE SPINS slot (the booster only affects base spins).
        boosterDisplay = new AmountDisplay("BOOSTER ACTIVE", 220f, 70f);
        boosterDisplay.setPosition(winDisplay.getX() - 20f, winDisplay.getY(Align.center), Align.right);
        boosterDisplay.setVisible(false);
        stage.addActor(boosterDisplay);

        // Bottom right: SPIN with RESET underneath. (this::spin passes the spin() method as the click action.)
        spinButton = new Button("SPIN", 220f, 60f, this::spin);
        spinButton.setPosition(reelScreen.getRight(), BUTTON_BAR_HEIGHT - 12f, Align.topRight);
        stage.addActor(spinButton);

        resetButton = new Button("RESET", 220f, 36f, this::reset);
        resetButton.setPosition(reelScreen.getRight(), spinButton.getY() - 8f, Align.topRight);
        stage.addActor(resetButton);

        // Bottom left: MENU with SHOP underneath (mirrors SPIN / RESET).
        menuButton = new Button("MENU", 220f, 60f, this::openMenu);
        menuButton.setPosition(reelScreen.getX(), BUTTON_BAR_HEIGHT - 12f, Align.topLeft);
        stage.addActor(menuButton);

        shopButton = new Button("SHOP", 220f, 36f, this::openShop);
        shopButton.setPosition(reelScreen.getX(), menuButton.getY() - 8f, Align.topLeft);
        stage.addActor(shopButton);

        // Bottom centre: BALANCE, in the middle of the gap between MENU and SPIN.
        balanceDisplay = new AmountDisplay("BALANCE", 260f, 80f);
        float gapCentreX = (menuButton.getRight() + spinButton.getX()) / 2f;
        balanceDisplay.setPosition(gapCentreX, BUTTON_BAR_HEIGHT / 2f, Align.center);
        stage.addActor(balanceDisplay);
        refreshBalance();

        // Top right corner: SIM (Monte Carlo simulator) and CONFIG, outside the reel area.
        simButton = new Button("SIM", 100f, 44f, this::openSim);
        simButton.setPosition(WORLD_WIDTH - MARGIN, WORLD_HEIGHT - TOP_BAR_HEIGHT / 2f, Align.right);
        stage.addActor(simButton);

        configButton = new Button("CONFIG", 100f, 44f, this::openConfig);
        configButton.setPosition(WORLD_WIDTH - MARGIN, WORLD_HEIGHT - 2.5f * TOP_BAR_HEIGHT / 2f, Align.right);
        stage.addActor(configButton);

        // Built once, added to / removed from the stage when opened / closed.
        infoPanel = new InfoPanel("GAME INFO", GameInfo.build(config, REELS, ROWS), this::closeMenu);
        Simulator simulator = new Simulator(math, REELS, ROWS);
        simPanel = new InfoPanel("MONTE CARLO SIMULATOR", new SimulatorContent(simulator,config), this::closeSim);
        configPanel = new InfoPanel("CONFIG", new ConfigContent(config), this::closeConfig, 1240f);   // wider: arrow buttons
    }

    // ---------------------------------------------------------------- round flow

    /** What a round in this mode costs, in cents. The price itself is maths: SlotMath.price. */
    private long cost(GameMode mode) {
        return Math.round(math.price(mode) * BET);
    }

    /** What one press of SPIN costs right now: the bet, or the booster price if a booster is on. */
    private long spinCost() {
        return cost(activeBooster);
    }

    private void spin() {
        if (spinning || isOverlayOpen() || !wallet.canAfford(spinCost())) {
            return;                          // can't spin while a round is running, a panel is open, or when broke
        }
        startRound(activeBooster);
    }

    /** Pays for and plays one round in the given mode (normal/booster spin, or a bought bonus). */
    private void startRound(GameMode mode) {
        spinning = true;
        spinButton.setEnabled(false);

        wallet.debit(cost(mode));            // the price leaves the balance the moment the round starts
        refreshBalance();
        shownWin = 0;
        winDisplay.setVisible(false);

        // The WHOLE round is decided here: base spin, avalanches AND the bonus. The screen only replays it.
        final RoundResult round = engine.playRound(rng, mode);
        final long totalWin = round.totalWin(BET);

        if (round.hasBaseSpin()) {
            reelScreen.playSpin(round.baseSpin, BET, this, () -> afterBaseSpin(round, totalWin));
        } else {
            reelScreen.setEmpty();           // bought bonus: no base spin, straight into free spins
            startFreeSpins(round, totalWin);
        }


    }

    private void afterBaseSpin(RoundResult round, long totalWin) {
        if (round.triggeredFreeSpins()) {
            startFreeSpins(round, totalWin);
        } else {
            finishRound(totalWin);
        }
    }

    // ---------------------------------------------------------------- free spins flow
    // Each step starts the next one from its "done" callback. It looks like recursion, but it isn't:
    // each call only schedules animations and returns; the next step runs frames later.

    private void startFreeSpins(RoundResult round, long totalWin) {
        FreeSpinsResult bonus = round.freeSpins;
        freeSpinsTotal = bonus.initialSpins;

        boosterDisplay.setVisible(false);    // its slot is used by FREE SPINS now
        if (round.hasBaseSpin()) {
            reelScreen.highlightSymbol(Symbol.SCATTER, round.baseSpin.finalGrid());
        }
        String title = bonus.superMode
                ? "SUPER FREE SPINS!\n" + freeSpinsTotal + " spins\nwilds are STICKY until they win"
                : "FREE SPINS!\n" + freeSpinsTotal + " spins";
        reelScreen.showMessage(title, 2.5f, () -> {
            reelScreen.clearHighlights();
            reelScreen.setFreeSpinsMode(true);
            shatterPanel.reset();
            shatterPanel.setVisible(true);
            freeSpinsDisplay.setVisible(true);
            multiplierDisplay.setText("x" + bonus.startMultiplier);
            multiplierDisplay.setVisible(true);
            playFreeSpin(bonus, 0, totalWin);
        });
    }

    private void playFreeSpin(FreeSpinsResult bonus, int index, long totalWin) {
        if (index >= bonus.spinCount()) {
            endFreeSpins(bonus, totalWin);
            return;
        }
        freeSpinsDisplay.setText((index + 1) + " / " + freeSpinsTotal);

        reelScreen.playSpin(bonus.spins.get(index), BET, this, () -> {
            int extra = bonus.extraSpinsAt(index);
            if (extra > 0) {
                freeSpinsTotal += extra;
                freeSpinsDisplay.setText((index + 1) + " / " + freeSpinsTotal);
                reelScreen.highlightSymbol(Symbol.SCATTER, bonus.spins.get(index).finalGrid());
                reelScreen.showMessage("+" + extra + " FREE SPINS", 1.8f, () -> {
                    reelScreen.clearHighlights();
                    playFreeSpin(bonus, index + 1, totalWin);
                });
            } else {
                playFreeSpin(bonus, index + 1, totalWin);
            }
        });
    }

    private void endFreeSpins(FreeSpinsResult bonus, long totalWin) {
        String text = "FREE SPINS WIN\n" + Wallet.format(bonus.totalWin(BET))
                + "\nmultiplier reached x" + bonus.finalMultiplier();
        if (bonus.superMode) {
            text += "\nsticky wilds left: " + bonus.finalStickyWilds();
        }
        reelScreen.showMessage(text, 3f, () -> {
            reelScreen.setFreeSpinsMode(false);
            reelScreen.clearStickyWilds();
            shatterPanel.setVisible(false);
            freeSpinsDisplay.setVisible(false);
            multiplierDisplay.setVisible(false);
            refreshBoosterDisplay();
            finishRound(totalWin);
        });
    }

    // ---------------------------------------------------------------- ReelScreen.SpinListener

    @Override
    public void onWinShown(long amountCents) {
        shownWin += amountCents;
        winDisplay.setAmount(shownWin);
        winDisplay.setVisible(true);
    }

    @Override
    public void onMultiplierChanged(int multiplier) {
        multiplierDisplay.setText("x" + multiplier);    // harmless in the base game: the display is hidden
    }

    @Override
    public void onShatterMeters(int[] meters) {
        shatterPanel.setMeters(meters);
    }

    @Override
    public void onShatterExplode(Symbol gem) {
        shatterPanel.explode(gem);
    }

    private void finishRound(long totalWin) {
        wallet.credit(totalWin);             // paid once, after everything was shown (base + bonus)
        refreshBalance();
        spinning = false;
        spinButton.setEnabled(true);
    }

    private void reset() {
        if (spinning) {
            return;                          // simplest safe rule: no reset mid-round
        }
        wallet.reset();
        reelScreen.clearActions();
        reelScreen.setEmpty();
        reelScreen.setFreeSpinsMode(false);
        reelScreen.clearStickyWilds();
        shatterPanel.reset();
        shatterPanel.setVisible(false);
        winDisplay.setVisible(false);
        freeSpinsDisplay.setVisible(false);
        multiplierDisplay.setVisible(false);
        activeBooster = GameMode.NORMAL;
        refreshBoosterDisplay();
        refreshBalance();
    }

    private void refreshBalance() {
        balanceDisplay.setAmount(wallet.getBalance());
        balanceDisplay.setWarning(!wallet.canAfford(spinCost()));
    }

    // ---------------------------------------------------------------- shop

    private void openShop() {
        shopPanel = new InfoPanel("SHOP",
                ShopContent.build(activeBooster, wallet, math, BET, spinning, this::onShopChoice),
                this::closeShop);
        stage.addActor(shopPanel);
        stage.setScrollFocus(shopPanel.getScrollPane());
    }

    private void closeShop() {
        if (shopPanel != null) {
            shopPanel.remove();
        }
    }

    /** Called by a shop button. A buy starts the bonus; a booster is switched on or off. */
    private void onShopChoice(GameMode mode) {
        if (spinning) {
            return;
        }
        closeShop();
        if (mode.isBuy()) {
            if (activeBooster == GameMode.NORMAL && wallet.canAfford(cost(mode))) {
                startRound(mode);
            }
        } else if (mode.isBooster()) {
            activeBooster = (activeBooster == mode) ? GameMode.NORMAL : mode;   // same one again = turn off
            refreshBoosterDisplay();
            refreshBalance();
        }
    }

    private void refreshBoosterDisplay() {
        boolean on = activeBooster != GameMode.NORMAL;
        if (on) {
            boosterDisplay.setText(activeBooster.displayName.replace(" Booster", "") + " " + Wallet.format(spinCost()));
        }
        boosterDisplay.setVisible(on);
    }

    // ---------------------------------------------------------------- info menu

    private void openMenu() {
        stage.addActor(infoPanel);                           // added last = drawn on top of everything
        stage.setScrollFocus(infoPanel.getScrollPane());     // mouse wheel scrolls the text
    }

    private void closeMenu() {
        infoPanel.remove();                                  // takes it off the stage (it isn't destroyed)
    }

    private boolean isMenuOpen() {
        return infoPanel.hasParent();                        // true while it's on the stage
    }

    private boolean isOverlayOpen() {
        return isMenuOpen() || simPanel.hasParent() || configPanel.hasParent()
                || (shopPanel != null && shopPanel.hasParent());
    }

    // ---------------------------------------------------------------- simulator

    private void openSim() {
        stage.addActor(simPanel);
        stage.setScrollFocus(simPanel.getScrollPane());
    }

    private void closeSim() {
        simPanel.remove();          // a running simulation keeps going in the background
        stage.setKeyboardFocus(null);
    }

    // ---------------------------------------------------------------- config

    private void openConfig() {
        stage.addActor(configPanel);
        stage.setScrollFocus(configPanel.getScrollPane());
    }

    private void closeConfig() {
        configPanel.remove();
        stage.setKeyboardFocus(null);
    }

    // ---------------------------------------------------------------- libGDX lifecycle

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            spin();                                          // spin() itself ignores it while a panel is open
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeMenu();
            closeShop();
            closeSim();
            closeConfig();
        }

        ScreenUtils.clear(0.08f, 0.08f, 0.10f, 1f);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        Assets.dispose();
    }
}