package org.example;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PBServerInterface extends Remote {
    Common.GetReply get(Common.GetArgs args) throws RemoteException;
    Common.PutAppendReply putAppend(Common.PutAppendArgs args) throws RemoteException;
}

