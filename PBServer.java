package org.example;

import org.example.Common.View;
import org.example.ViewServerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.NotBoundException; // Импортируем NotBoundException
import java.util.HashMap;
import java.util.Map;

public class PBServer extends UnicastRemoteObject implements PBServerInterface {
    private final Map<String, String> keyValue = new HashMap<>();
    private final Map<String, Common.PutAppendArgs> lastArgs = new HashMap<>();
    private final String me;
    private final ViewServerInterface vs;
    private Common.View view;

    public PBServer(String me, ViewServerInterface vs) throws RemoteException {
        this.me = me;
        this.vs = vs;
        this.view = new Common.View(0, "", ""); // Начальный номер представления - 0, primary и backup - пустые строки
    }

    @Override
    public Common.GetReply get(Common.GetArgs args) throws RemoteException {
        Common.GetReply reply = new Common.GetReply();
        if (!view.getPrimary().equals(me)) {
            reply.err = Common.ErrWrongServer;
            return reply;
        }
        reply.value = keyValue.getOrDefault(args.key, "");
        return reply;
    }

    @Override
    public Common.PutAppendReply putAppend(Common.PutAppendArgs args) throws RemoteException {
        Common.PutAppendReply reply = new Common.PutAppendReply();

        if (!view.getPrimary().equals(me)) {
            reply.err = Common.ErrWrongServer;
            return reply;
        }

        // Проверка на дублирующий запрос
        if (args.equals(lastArgs.get(args.from))) {
            return reply;
        }

        // Если есть резервный сервер, пересылаем ему команду
        if (view.getBackup() != null && !view.getBackup().isEmpty()) {
            try {
                Registry registry = LocateRegistry.getRegistry(view.getBackup());
                PBServerInterface backup = (PBServerInterface) registry.lookup("PBServer");
                backup.putAppend(args);
            } catch (NotBoundException e) {
                System.err.println("Backup server not bound in registry: " + e.getMessage());
            }
        }

        // Обработка операции Put или Append
        if (Common.Append.equals(args.operation) && keyValue.containsKey(args.key)) {
            keyValue.put(args.key, keyValue.get(args.key) + args.value);
        } else {
            keyValue.put(args.key, args.value);
        }
        lastArgs.put(args.from, args);
        return reply;
    }

    private void updateView() throws RemoteException {
        Common.PingArgs args = new Common.PingArgs(me, view.getViewnum()); // Создаем PingArgs с текущим сервером и номером представления
        view = vs.ping(args); // Передаем PingArgs вместо int
    }

    public static void main(String[] args) {
        try {
            String serverName = args[0];
            Registry registry = LocateRegistry.getRegistry("localhost", 1112);
            ViewServerInterface vs = (ViewServerInterface) registry.lookup("ViewServer");
            PBServer server = new PBServer(serverName, vs);
            registry.rebind(serverName, server); // Используем уникальное имя сервера
            System.out.println("PBServer started as " + serverName);

            while (true) {
                server.updateView();
                Thread.sleep(Common.PING_INTERVAL); // Используем значение PING_INTERVAL из Common
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println("ViewServer not bound in registry: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
