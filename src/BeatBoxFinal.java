import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.event.*;
public class BeatBoxFinal {
    public static final int AMOUNT_INSTRUMENTS = 16;
    public static final int FIELD_SIZE = 256;
    JFrame theFrame;
    JPanel mainPanel;
    JList incomingList;
    JTextField userMessage;
    ArrayList<JCheckBox> checkboxList;
    int nextNum;
    Vector<String> listVector = new Vector<>();
    String userName;
    ObjectOutputStream out;
    ObjectInputStream in;
    HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    Sequencer sequencer;
    Sequence sequence;
    Sequence mySequence = null;
    Track track;
    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
            "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Ho Conga"};
    int[] instruments = {35, 42, 46, 38, 49, 39, 50,60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new BeatBoxFinal().startUp("Music");
    }
    public void startUp(String name){
        userName = name;
        try {
            Socket sock = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(sock.getOutputStream());
            in = new ObjectInputStream(sock.getInputStream());
            Thread remote = new Thread(new RemoteReader());
            remote.start();
        } catch (Exception ex){
            System.out.println("couldn't connect - you'll have to play alone.");
        }
        setUpMidi();
        buildGUI();
    }

    public void buildGUI(){
        theFrame = new JFrame("Cyber BeatBox");
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        checkboxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);
        JButton start = new JButton("Start");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);
        JButton stop = new JButton("Stop");
        start.addActionListener(new MyStopListener());
        buttonBox.add(stop);
        JButton upTempo = new JButton("Tempo Up");
        start.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);
        JButton downTempo = new JButton("Tempo down");
        start.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);
        JButton sendIt = new JButton("sendIt");
        start.addActionListener(new MySendListener());
        buttonBox.add(sendIt);
        userMessage = new JTextField();
        buttonBox.add(userMessage);
        incomingList = new JList();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);
        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < AMOUNT_INSTRUMENTS; i++){
            nameBox.add(new Label(instrumentNames[i]));
        }
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);
        theFrame.getContentPane().add(background);
        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        for (int i = 0; i < AMOUNT_INSTRUMENTS * AMOUNT_INSTRUMENTS; i++){
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }
        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }

    public void setUpMidi(){
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildTrackAndStart(){
        ArrayList<Integer> trackList;
        sequence.deleteTrack(track);
        track = sequence.createTrack();
        for (int i = 0; i < AMOUNT_INSTRUMENTS; i++){
            trackList = new ArrayList<>();
            for (int j = 0; j < AMOUNT_INSTRUMENTS; j++){
                JCheckBox jc = checkboxList.get(j + (AMOUNT_INSTRUMENTS * i));
                if (jc.isSelected()){
                    int key = instruments[i];
                    trackList.add(key);
                } else {
                    trackList.add(null);
                }
            }
            makeTrack(trackList);
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a){
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a){
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a){
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a){
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }

    public class MySendListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            boolean[] checkboxState = new boolean[FIELD_SIZE];
            for (int i = 0; i < FIELD_SIZE; i++){
                JCheckBox check = checkboxList.get(i);
                if (check.isSelected()){
                    checkboxState[i] = true;
                }
            }
            try {
                out.writeObject(userName + nextNum + ": " + userMessage.getText());
                out.writeObject(checkboxState);
            } catch (Exception ex){
                System.out.println("Sorry dude. Could not send it to the server.");
            }
            userMessage.setText("");
        }
    }

    public class MyListSelectionListener implements ListSelectionListener{
        public void valueChanged(ListSelectionEvent le){
            if (!le.getValueIsAdjusting()){
                String selected = (String) incomingList.getSelectedValue();
                if (selected != null){
                    boolean[] selectedState = otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class RemoteReader implements Runnable{
        boolean[] checkboxState = null;
        Object obj = null;
        public void run(){
            try {
                while ((obj = in.readObject()) != null){
                    System.out.println("got an object from server");
                    System.out.println(obj.getClass());
                    String nameToShow = (String) obj;
                    checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
    public void changeSequence(boolean[] checkboxState){
        for (int i = 0; i < FIELD_SIZE; i++){
            JCheckBox check = checkboxList.get(i);
            if (checkboxState[i]){
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
    }
    public void makeTrack(ArrayList<Integer> list){
        Iterator it = list.iterator();
        for (int i = 0; i < AMOUNT_INSTRUMENTS; i++){
            Integer num = (Integer) it.next();
            if (num != null){
                int numKey = num.intValue();
                track.add(makeEvent(144, 9, numKey, 100, i));
                track.add(makeEvent(128, 9, numKey, 100, i + 1));
            }
        }
    }
    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);
        } catch (Exception e){
            e.printStackTrace();
        }
        return event;
    }

    public class MyReadInListener implements ActionListener{
        public void actionPerformed(ActionEvent a){
            boolean[] checkboxState = null;
            try {
                FileInputStream fileInputStream = new FileInputStream(new File("Checkbox.ser"));
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                checkboxState = (boolean[]) objectInputStream.readObject();
            } catch (Exception ex){
                ex.printStackTrace();
            }
            for (int i = 0; i < FIELD_SIZE; i++){
                JCheckBox check = (JCheckBox) checkboxList.get(i);
                if (checkboxState[i]){
                    check.setSelected(true);
                } else {
                    check.setSelected(false);
                }
            }
            sequencer.stop();
            buildTrackAndStart();
        }
    }
}