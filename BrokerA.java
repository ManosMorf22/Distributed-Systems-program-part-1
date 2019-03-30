import java.io.*;
import java.net.*;
import java.util.ArrayList;
//hash bus lineid
//topic or key LineId
//value busPosition

public  class BrokerA{
    private static ArrayList<BusLine> busLines = new ArrayList<>();
    private static ArrayList<BusLine> responsibleLines = new ArrayList<>();
    private static ArrayList<BusPosition> busPositions = new ArrayList<>();
    private static ArrayList<BusPosition> datafrompublisher = new ArrayList<>();

    static ArrayList<BusLine> getResponsibleLines(){
        return  responsibleLines;
    }

    public static class ComunicationWithPublisherThread implements Runnable {
        private Socket socket;

        ComunicationWithPublisherThread (Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                for(BusLine  b:busLines) if(Integer.parseInt(Utilities.MD5(b.getLineId()))<Integer.parseInt(Utilities.MD5(socket.getInetAddress().toString() + "4321"))) responsibleLines.add(b);

                out.writeObject("I am responsible for these keys:\n");
                for (BusLine  rl:responsibleLines) {
                    out.writeObject(rl.getRoute().getRouteDescription()+"\n");
                    out.writeObject(rl.getLineId()+"\n\n");
                }
                String message = (String) in.readObject();//sypose  that the pub send the data in this way
                while (!message.equals("bye")) {
                   String lineId=(String) in.readObject();
                   int x=(Integer)in.readObject();//check me
                    int y=(Integer)in.readObject();//check me
                    for(BusPosition bp:busPositions) if(bp.getLatitude() == y&&bp.getLongitude() == x&&bp.getLongitude() == x&&bp.getBus().getLineId().equals(lineId)) datafrompublisher.add(bp);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    public static class ComunicationWithConsumerThread implements  Runnable{
        private Socket socket;

        ComunicationWithConsumerThread(Socket socket){
            this.socket=socket;
        }
        public void run(){
            try {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject("I am broker A and I am responsible for these keys:\n");
                //String message = (String) in.readObject();
                for (BusLine  rl:responsibleLines) {
                    out.writeObject(rl.getRoute().getRouteDescription() + "\n");
                    out.writeObject(rl.getLineId()+"\n\n");
                }
                //the other brokers
                out.writeObject("Broker B is responsible for these keys :\n");
                ArrayList<BusLine> bB=BrokerB.getResponsibleLines();
                for(BusLine rl:bB){
                    out.writeObject(rl.getRoute().getRouteDescription() + "\n");
                    out.writeObject(rl.getLineId() + "\n\n");
                }
                out.writeObject("Broker C is responsible for these keys :\n");
                ArrayList<BusLine> bC=BrokerC.getResponsibleLines();
                for(BusLine rl:bC){
                    out.writeObject(rl.getRoute().getRouteDescription()+"\n");
                    out.writeObject(rl.getLineId()+"\n\n");
                }

                //
                try {
                    String lineId = (String) in.readObject();
                    out.writeObject("My responsibilities :\n");
                    while(!lineId.equals("bye")) {
                        for (BusLine bl : responsibleLines)
                            if (bl.getLineId().equals(lineId))//only for my responsibility
                                for (BusPosition bp : busPositions) {
                                    if (bp.getBus().getLineId().equals(lineId)) {
                                        out.writeObject("Bus from line " + lineId + "\n");
                                        out.writeObject("Longitude " + bp.getLongitude() + " Latitude " + bp.getLatitude() + "\n");
                                    }
                                }
                        lineId = (String) in.readObject();
                    }
                }catch(ClassNotFoundException e){
                    e.printStackTrace();
                }
            }catch(IOException  e){
                e.printStackTrace();
            }

        }
    }
    public static void main(String[] args) throws IOException{
        new Utilities().openServer(5090);
    }
}

