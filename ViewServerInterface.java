package org.example;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ViewServerInterface extends Remote {
    Common.View ping(Common.PingArgs args) throws RemoteException;
    Common.View get(Common.GetArgs args) throws RemoteException;
    String getPrimary() throws RemoteException; // Добавляем метод getPrimary
}
