package pmel.sdig.las.shared.autobean;

import java.util.Map;

public class ConfigSet {

    Map<String, Config> config;

    public Map<String, Config> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Config> config) {
        this.config = config;
    }
}
