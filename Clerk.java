package org.example;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.util.Random;
import org.example.ViewServerInterface;

public class Clerk {
    private ViewServerInterface vs;
    private String me;
    private String primary;

    public Clerk(ViewServerInterface vs, String me) {
        this.vs = vs;
        this.me = me + new Random().nextInt(1000);
        this.primary = "";
    }

    public String get(String key) throws RemoteException {
        while (primary == null || primary.isEmpty()) {
            primary = vs.getPrimary();
            try {
                Thread.sleep(100); // ожидание перед повторной проверкой
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Common.GetArgs args = new Common.GetArgs(key);
        Common.GetReply reply;

        do {
            reply = sendGet(args);
            if (Common.ErrWrongServer.equals(reply.err)) {
                primary = vs.getPrimary();
            }
        } while (!Common.OK.equals(reply.err));

        return reply.value;
    }

    private Common.GetReply sendGet(Common.GetArgs args) {
        try {
            Registry registry = LocateRegistry.getRegistry(primary);
            PBServerInterface server = (PBServerInterface) registry.lookup("PBServer");
            return server.get(args);
        } catch (Exception e) {
            return new Common.GetReply();
        }
    }

    public void put(String key, String value) throws RemoteException {
        putAppend(key, value, Common.Put);
    }

    public void append(String key, String value) throws RemoteException {
        putAppend(key, value, Common.Append);
    }

    private void putAppend(String key, String value, String op) throws RemoteException {
        while (primary == null || primary.isEmpty()) {
            primary = vs.getPrimary();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Common.PutAppendArgs args = new Common.PutAppendArgs(key, value, op, me);
        Common.PutAppendReply reply;

        do {
            reply = sendPutAppend(args);
            if (Common.ErrWrongServer.equals(reply.err)) {
                primary = vs.getPrimary();
            }
        } while (!Common.OK.equals(reply.err));
    }

    private Common.PutAppendReply sendPutAppend(Common.PutAppendArgs args) {
        try {
            Registry registry = LocateRegistry.getRegistry(primary);
            PBServerInterface server = (PBServerInterface) registry.lookup("PBServer");
            return server.putAppend(args);
        } catch (Exception e) {
            return new Common.PutAppendReply();
        }
    }
}
