package dev.pallux.amethysttools.models;

public enum ToolType {
    TREE_CHOPPER("tree-chopper"),
    SELL_AXE("sell-axe"),
    PICKAXE("pickaxe"),
    BUCKET("bucket"),
    TORCH("torch"),
    ROCKET("rocket");

    private final String configName;

    ToolType(String configName) {
        this.configName = configName;
    }

    public String getConfigName() {
        return configName;
    }

    public static ToolType fromConfigName(String configName) {
        for (ToolType type : values()) {
            if (type.configName.equals(configName)) {
                return type;
            }
        }
        return null;
    }

    public String getDisplayName() {
        return switch (this) {
            case TREE_CHOPPER -> "Amethyst Tree Chopper";
            case SELL_AXE -> "Amethyst Sell Axe";
            case PICKAXE -> "Amethyst Pickaxe";
            case BUCKET -> "Amethyst Bucket";
            case TORCH -> "Amethyst Torch";
            case ROCKET -> "Amethyst Rocket";
        };
    }
}