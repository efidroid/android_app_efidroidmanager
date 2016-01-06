package manager.efidroid.org.efidroidmanager.models;

public class OperatingSystem {
    private String mName = "";
    private String mDescription = "";

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }
}
