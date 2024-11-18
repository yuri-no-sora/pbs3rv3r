package org.example;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

public class ViewServer extends UnicastRemoteObject implements ViewServerInterface {
    private Common.View view;
    private Map<String, Instant> lastPingTime = new HashMap<>();
    private boolean hasAcknowledged;
    private String idleServer;

    public ViewServer() throws RemoteException {
        view = new Common.View(0, "", "");
        hasAcknowledged = false;
    }

    private synchronized void changeView(String primary, String backup) {
        view = new Common.View(view.getViewnum() + 1, primary, backup);
        hasAcknowledged = false;
    }

    @Override
    public synchronized Common.View ping(Common.PingArgs args) throws RemoteException {
        String client = args.me;
        int viewnum = args.viewnum;

        lastPingTime.put(client, Instant.now());

        if (client.equals(view.getPrimary())) {
            if (viewnum == 0) {
                changeView(view.getBackup(), "");
            } else if (view.getViewnum() == viewnum) {
                hasAcknowledged = true;
            }
        } else if (client.equals(view.getBackup())) {
            if (viewnum == 0 && hasAcknowledged) {
                changeView(view.getPrimary(), idleServer);
            }
        } else {
            if (view.getViewnum() == 0) {
                changeView(client, "");
            } else {
                idleServer = client;
            }
        }
        return view;
    }

    @Override
    public synchronized Common.View get(Common.GetArgs args) throws RemoteException {
        return view;
    }

    @Override
    public String getPrimary() throws RemoteException {
        return view.getPrimary(); // Реализация getPrimary, возвращающая текущий primary сервер
    }

    private boolean isOverTime(Instant lastPing, Instant currentTime) {
        return lastPing == null || currentTime.toEpochMilli() - lastPing.toEpochMilli() > Common.DEAD_PINGS * Common.PING_INTERVAL;
    }

    private void tick() {
        Instant currentTime = Instant.now();

        synchronized (this) {
            if (isOverTime(lastPingTime.get(view.getPrimary()), currentTime)) {
                if (hasAcknowledged) {
                    changeView(view.getBackup(), idleServer);
                    idleServer = null;
                }
            } else if (isOverTime(lastPingTime.get(view.getBackup()), currentTime)) {
                if (hasAcknowledged) {
                    changeView(view.getPrimary(), idleServer);
                    idleServer = null;
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            ViewServer server = new ViewServer();
            Registry registry = LocateRegistry.createRegistry(1112);
            registry.rebind("ViewServer", server);
            System.out.println("ViewServer started");

            // Run tick periodically
            while (true) {
                server.tick();
                Thread.sleep(Common.PING_INTERVAL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
