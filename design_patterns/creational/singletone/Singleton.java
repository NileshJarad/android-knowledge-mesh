public final class Singleton {
    private static volatile Singleton instance;

    public static Singleton getInstance() {

        /**
         * This is DCL method more details at https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
         */
        Singleton result = instance;
        if (result != null) {
            return result;
        }

        synchronized (Singleton.class) {
            if (instance == null) {
                instance = new Singleton();
            }
            return instance;
        }
    }

    public static void main() {

    }
}

 class DemoMultiThread {
    public static void main(String[] args) {
        Thread threadFoo = new Thread(new Thread1());
        Thread threadBar = new Thread(new Thread2());
        threadFoo.start();
        threadBar.start();
    }

    static class Thread1 implements Runnable {
        @Override
        public void run() {
            Singleton singleton = Singleton.getInstance();
            System.out.println(singleton);
        }
    }

    static class Thread2 implements Runnable {
        @Override
        public void run() {
            Singleton singleton = Singleton.getInstance();
            System.out.println(singleton);
        }
    }
}
