package org.example;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

class Client {
    private String me;         // имя клиента (host:port)
    private String server;     // имя сервера ViewServer (host:port)

    public Client(String me, String server) {
        this.me = me;
        this.server = server;
    }

    private boolean call(String rpcName, Object[] args, Object reply) {
        try {
            // Подключаемся к ViewServer по указанному порту
            Registry registry = LocateRegistry.getRegistry(server, 1112);
            ViewServerInterface vs = (ViewServerInterface) registry.lookup("ViewServer");

            if (rpcName.equals("Ping")) {
                Common.PingArgs pingArgs = (Common.PingArgs) args[0];
                Common.PingReply pingReply = (Common.PingReply) reply;
                pingReply.setView(vs.ping(pingArgs));
            } else if (rpcName.equals("Get")) {
                Common.GetReply getReply = (Common.GetReply) reply;
                getReply.setView(vs.get(new Common.GetArgs()));
            }
            return true;
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Common.View ping(int viewnum) throws RemoteException {
        Common.PingArgs args = new Common.PingArgs(me, viewnum);
        Common.PingReply reply = new Common.PingReply();
        boolean ok = call("Ping", new Object[]{args}, reply);
        if (!ok) {
            throw new RemoteException("Ping(" + viewnum + ") failed");
        }
        return reply.getView();
    }

    public Common.View get() throws RemoteException {
        Common.GetReply reply = new Common.GetReply();
        boolean ok = call("Get", new Object[]{}, reply);
        if (!ok) {
            throw new RemoteException("Get failed");
        }
        return reply.getView();
    }

    public String primary() throws RemoteException {
        Common.View v = get();
        return v.getPrimary();
    }

    public static void main(String[] args) {
        try {
            // Задаём имя клиента и адрес ViewServer
            String clientName = "client1";
            String viewServerHost = "localhost";

            // Создаём экземпляр клиента
            Client client = new Client(clientName, viewServerHost);

            // Тест вызовов
            System.out.println("Connecting to the primary server...");
            System.out.println("The Primary Server: " + client.primary());

            // Получаем текущую View и выводим информацию
            Common.View view = client.get();
                System.out.println("Current View number: " + view.getViewnum());
            System.out.println("Current Primary Server: " + view.getPrimary());
            System.out.println("Current Backup Server: " + view.getBackup());

        } catch (RemoteException e) {
            System.err.println("Error when connecting to the server: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unknown Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
