package pmel.sdig.las

class Pulse {

    String state; // Should be one of PulseType
    List<String> messages = new ArrayList<String>()
    String pulseFile
    String ferretScript
    String downloadFile
    String pid  // Comes in as a string from the process table parsing
    String time // Elapsed CPU time from process table as HH:MM:SS
    String memory
    boolean hasPulse // true if pulse file exists a moment pulse is checked, false if checking creates the file

    public void addMessage(String message) {
        messages.add(message);
    }
}
