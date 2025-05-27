package re.imc.geysermodelengine.managers.server;

public class ServerManager {

    private final ServerData serverData;

    public ServerManager() {
        this.serverData = new ServerData();
    }

    public ServerData getServerData() {
        return serverData;
    }
}
