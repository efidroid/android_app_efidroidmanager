package manager.efidroid.org.efidroidmanager.models;

import org.ini4j.Ini;

public class OperatingSystem {
    private String mName = "";
    private String mDescription = "";
    private final Ini mIni;

    public OperatingSystem(Ini ini) {
        mIni = ini;
    }

    public String getName() {
        return mIni.get("config", "name");
    }

    public void setName(String name) {
        mIni.put("config", "name", name);
    }

    public String getDescription() {
        return mIni.get("config", "description");
    }

    public void setDescription(String description) {
        mIni.put("config", "description", description);
    }
}
