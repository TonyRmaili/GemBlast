package gemblast;

import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Content of the CONFIG panel (the dynamic weight adjuster comes later).
 * It edits the ValueConfig object directly, so the game and the simulator use the new values
 * from the next spin; config.save() writes them back to weights.json.
 */
public class ConfigContent extends Table {

    private final ValueConfig config;

    public ConfigContent(ValueConfig config) {
        this.config = config;
        // build the configuration UI here
    }
}
