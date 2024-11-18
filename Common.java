package org.example;

import java.io.Serializable;

// Константы и данные для общего использования в проекте
public class Common {
    public static final String OK = "OK";
    public static final String ErrNoKey = "ErrNoKey";
    public static final String ErrWrongServer = "ErrWrongServer";
    public static final String Put = "Put";
    public static final String Append = "Append";
    public static final int PING_INTERVAL = 100; // milliseconds
    public static final int DEAD_PINGS = 5;

    // Класс View
    public static class View implements Serializable {
        private int viewnum;
        private String primary;
        private String backup;

        public View(int viewnum, String primary, String backup) {
            this.viewnum = viewnum;
            this.primary = primary;
            this.backup = backup;
        }

        public int getViewnum() { return viewnum; }
        public String getPrimary() { return primary; }
        public String getBackup() { return backup; }
    }

    // Классы для пинга и получения данных
    public static class PingArgs implements Serializable {
        public String me;
        public int viewnum;

        public PingArgs(String me, int viewnum) {
            this.me = me;
            this.viewnum = viewnum;
        }
    }

    public static class PingReply implements Serializable {
        private View view;

        public View getView() { return view; }
        public void setView(View view) { this.view = view; }
    }

    public static class GetArgs implements Serializable {
        public String key;

        public GetArgs(String key) {
            this.key = key;
        }
        // Конструктор без параметров (добавить)
        public GetArgs() {
            this.key = null;
        }


    }



    public static class GetReply implements Serializable {
        public String err;
        public String value;
        private View view;

        public View getView() { return view; }
        public void setView(View view) { this.view = view; }
    }

    // Классы для операций Put и Append
    public static class PutAppendArgs implements Serializable {
        public String key;
        public String value;
        public String operation;
        public String from;

        public PutAppendArgs(String key, String value, String operation, String from) {
            this.key = key;
            this.value = value;
            this.operation = operation;
            this.from = from;
        }
    }

    public static class PutAppendReply implements Serializable {
        public String err;

        public PutAppendReply() {
            this.err = OK;
        }
    }
}
